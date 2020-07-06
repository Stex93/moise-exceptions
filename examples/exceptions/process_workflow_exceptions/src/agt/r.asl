// Agent sample_agent in project parseInt

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

+!getAmountAsString
	<- .wait(1000);
	   enableInput;
	   println("Getting amount...");
	   .wait({+inputReceived});
	   disableInput;
	   println("Amount received!").

// uncomment the include below to have an agent compliant with its organisation
{ include("$moiseJar/asl/org-obedient.asl") }
