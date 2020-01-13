package playground.vsp.zzArchive.bvwpOld;

@Deprecated
public class IllustrationAP200PV {

	private static Values economicValues;
	private static ScenarioForEvalData nullfall;
	private static ScenarioForEvalData planfall;

	public static void main(String[] args) {

		// create the economic values
		economicValues = EconomicValues.createEconomicValuesAP200PV();

		runInduced();
		runModeSwitch();
		runMixed();

	}

	private static void runInduced() {
		System.out.println("\n**********************************************************************************************************************************");
		System.out.println("**********************************************************************************************************************************");
		System.out.println("Running induced scenario...\n");

		// create the base case:
		nullfall = ScenarioAP200PV.createNullfall();

		// create the policy case:
		planfall = ScenarioAP200PV.createPlanfallInduced(nullfall);

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
		nullfall = ScenarioAP200PV.createNullfall();

		// create the policy case:
		planfall = ScenarioAP200PV.createPlanfallModeSwitch(nullfall);

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
		nullfall = ScenarioAP200PV.createNullfall();

		// create the policy case:
		planfall = ScenarioAP200PV.createPlanfallMixed(nullfall);

		runRoH();
		runBVWP2003();
		runBVWP2010();
		runBVWP2015();

	}

	private static void runRoH() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ;		
	}

	private static void runBVWP2003() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesBVWP2003();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ;
	}

	private static void runBVWP2010() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesBVWP2010();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall);
	}

	private static void runBVWP2015() {
		System.out.println("\n==================================================================================================================================");

		UtilityChanges utilityChanges = new UtilityChangesBVWP2015();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ;
	}

}
