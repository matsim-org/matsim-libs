package playground.kai.bvwp;


public class IllustrationAP200PV {

	private static Values economicValues;
	private static ScenarioForEval nullfall;
	private static ScenarioForEval planfall;

	public static void main(String[] args) {

		// create the economic values
		economicValues = IllustrationAP200PVEconomicValues.createEconomicValues1();

		runInduced();
		runModeSwitch();
		runMixed();

	}

	private static void runInduced() {
		System.out.println("\n**********************************************************************************************************************************");
		System.out.println("**********************************************************************************************************************************");
		System.out.println("Running induced scenario...\n");

		// create the base case:
		nullfall = IllustrationAP200PVScenarioInduced.createNullfall1();

		// create the policy case:
		planfall = IllustrationAP200PVScenarioInduced.createPlanfall1(nullfall);

		runRoH();
		runBVWP2003();
		runBVWP2010();
		runBVWP2015();
	}

	private static void runModeSwitch() {
		System.out.println("\n**********************************************************************************************************************************");
		System.out.println("**********************************************************************************************************************************");
		System.out.println("Running mode switch scenario...\n");

		// create the base case:
		nullfall = IllustrationAP200PVScenarioModeSwitch.createNullfall1();

		// create the policy case:
		planfall = IllustrationAP200PVScenarioModeSwitch.createPlanfall1(nullfall);

		runRoH();
		runBVWP2003();
		runBVWP2010();
		runBVWP2015();
	}

	private static void runMixed() {
		System.out.println("\n**********************************************************************************************************************************");
		System.out.println("**********************************************************************************************************************************");
		System.out.println("Running mixed scenario...\n");

		// create the base case:
		nullfall = IllustrationAP200PVScenarioMixed.createNullfall1();

		// create the policy case:
		planfall = IllustrationAP200PVScenarioMixed.createPlanfall1(nullfall);

		runRoH();
		runBVWP2003();
		runBVWP2010();
		runBVWP2015();

	}

	private static void runRoH() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;		
	}

	private static void runBVWP2003() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesBVWP2003();
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
	}

	private static void runBVWP2010() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesBVWP2010();
		utilityChanges.utilityChange(economicValues, nullfall, planfall);
	}

	private static void runBVWP2015() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesBVWP2015();
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
	}

}
