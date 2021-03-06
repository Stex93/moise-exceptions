package ora4mas.light;

import java.util.logging.Logger;

import cartago.CartagoException;
import cartago.OPERATION;
import jason.util.Config;
import moise.common.MoiseConsistencyException;
import moise.common.MoiseException;
import moise.os.OS;
import moise.os.fs.FS;
import moise.os.fs.Goal;
import moise.os.fs.Mission;
import moise.os.fs.Plan;
import moise.os.fs.Plan.PlanOpType;
import moise.os.ns.NS;
import npl.NormativeFailureException;
import npl.parser.ParseException;
import ora4mas.nopl.JasonTermWrapper;
import ora4mas.nopl.SchemeBoard;
import ora4mas.nopl.WebInterface;
import ora4mas.nopl.oe.Scheme;

/**
 * Artifact to manage a scheme instance.
 * <br/><br/>
 *
 * <b>Operations</b> (see details in the methods list below):
 * <ul>
 * <li>commitMission
 * <li>leaveMission
 * <li>goalAchieved
 * <li>setArgumentValue
 * <li>resetGoal
 * <li>destroy
 * </ul>
 *
 * <b>Observable properties</b>:
 * <ul>
 * <li>commitment(ag,mission,sch): agent ag is committed to the mission in the scheme (we have as many obs prop as commitments).</br>
 *     e.g. <code>commitment(bob,mission1,s1)</code>
 * <li>groups: a list of groups responsible for the scheme.</br>
 *     e.g. <code>groups([g1])</code>
 * <li>goalState(schId, goal, list of committed agents, list of agents that performed the goal, state); where states are: waiting, enabled, satisfied).</br>
 *     e.g. <code>goalState(s1,g5,[alice,bob],[alice],satisfied)</code>
 * <li>specification: the specification of the scheme in the OS (a prolog like representation).
 * <li>obligation(ag,reason,goal,deadline): current active obligations.</br>
 *     e.g. <code>obligation(bob,ngoal(s1,mission1,g5),done(s1,bid,bob),1475417322254)</code>
 * <li>permission(ag,reason,goal,deadline): current active permission.</br>
 *
 * <li>goalArgument(schemeId, goalId, argId, value): value of goals' arguments, defined by the operation setArgumentValue</br>
 *     e.g. <code>goalArgument(sch1, winner, "W", "Bob")</code>
 * </ul>
 *
 * <b>Signals</b> (obligation o has the form: obligation(to whom, maintenance condition, what, deadline)):
 * <ul>
 * <li>oblCreated(o): the obligation <i>o</i> is created.
 * <li>oblFulfilled(o): the obligation <i>o</i> is fulfilled
 * <li>oblUnfulfilled(o): the obligation <i>o</i> is unfulfilled (e.g. by timeout).
 * <li>oblInactive(o): the obligation <i>o</i> is inactive (e.g. its maintenance condition does not hold anymore).</br>
 *    e.g. <code>o = obligation(Ag,_,done(Sch,bid,Ag), TTF)</code> if the <code>bid</code> is a performance goal and
 *         <code>o = obligation(Ag,_,satisfied(Sch,bid), TTF)</code> if the <code>bid</code> is an achievement goal.
 * <li>normFailure(f): the failure <i>f</i> has happened (e.g. due some regimentation).</br>
 *    e.g. <code>f = fail(mission_permission(Ag,M,Sch))</code>. The f comes from the normative program.
 * </ul>
 *
 * @navassoc - specification - moise.os.fs.Scheme
 * @see moise.os.fs.Scheme
 * @author Jomi
 */
public class LightSchemeBoard extends SchemeBoard {

    protected Logger logger = Logger.getLogger(LightSchemeBoard.class.getName());

    protected String ALL_GOALS = "all_goals";
    
    /**
     * Initialises the scheme artifact
     *
     */
    public void init() throws ParseException, MoiseException {
        OS os = new OS();
        FS fs = new FS(os);
        os.setFS(fs);
        NS ns = new NS(os);
        ns.setProperty("default_management", "ignore");
        os.setNS(ns);
        
        spec = new moise.os.fs.Scheme("untyped", fs);
        spec.setRoot(new Goal(ALL_GOALS));
        spec.getRoot().setPlan(new Plan(PlanOpType.parallel, spec, ALL_GOALS));
        fs.addScheme(spec);

        final String schId = getId().getName();
        orgState   = new Scheme(spec, schId);

        oeId = getCreatorId().getWorkspaceId().getName();

        // load normative program
        initNormativeEngine(os, "scheme(untyped)");
        installNormativeSignaler();
        initWspRuleEngine();

        // observable properties
        updateGoalStateObsProp();
        updateGoalArgsObsProp();
        defineObsProperty(obsPropGroups,  getSchState().getResponsibleGroupsAsProlog());
        defineObsProperty(obsPropSpec, new JasonTermWrapper(spec.getAsProlog()));

        if (! "false".equals(Config.get().getProperty(Config.START_WEB_OI))) {
            WebInterface w = WebInterface.get();
            try {
                w.registerOEBrowserView(oeId, "/scheme/", schId, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        schBoards.add(this);
    }
    
    @OPERATION public void addGoal(String goalId, Object[] deps) throws MoiseException, ParseException, NormativeFailureException {
    	Goal g = getOrCreateGoal(goalId);
		spec.getRoot().getPlan().addSubGoal(goalId);
		
		for (Object o: deps) {
			g.addDependence(getOrCreateGoal(o.toString()));			
		}
        updateGoalStateObsProp();
        postReorgUpdates(spec.getFS().getOS(), "scheme(untyped)", "fs");
        getObsProperty(obsPropSpec).updateValue(new JasonTermWrapper(spec.getAsProlog()));
    }
    
    Goal getOrCreateGoal(String goalId) throws MoiseConsistencyException {
    	Goal g = spec.getGoal(goalId);
    	if (g == null) {
    		g = new Goal(goalId);      		
    		spec.addGoal(g);
    		spec.getRoot().getPlan().addSubGoal(goalId);
    		g.setInPlan(spec.getRoot().getPlan());
    		
    	}
		// create a corresponding mission
		Mission m = spec.getMission(goalId);
		if (m == null) {
			m = new Mission(goalId, spec);
    		spec.addMission(m);
		}
		m.addGoal(goalId);

    	return g;
    }

    @OPERATION public void commitGoal(String goalMission) throws CartagoException {
    	commitMission(getOpUserName(), goalMission);
    }

    @Override
    protected boolean addMissionsInDot() {
    	return false;
    }
    
}
