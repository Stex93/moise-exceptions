
+!receiveInfo
	 : play(Ag,creditCardEmployee2,gcc)
	<- println("Getting credit card balance...");
	   getBalance(Balance);
	   println("Balance: ",Balance);
	   .send(Ag,tell,balance(Balance)).
	   
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
{ include("$moiseJar/asl/org-obedient.asl") }
