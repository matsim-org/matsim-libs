package playground.vsp.bvwp;


public class IllustrationZielnetz {

	private static Values economicValues;
	private static ScenarioForEvalData nullfall;
	private static ScenarioForEvalData planfall;

	final static String divider = "\n==============================================================================\n";

	public static void main(String[] args) {

		// create the economic values
		economicValues = EconomicValues.createEconomicValuesZielnetz();

		// create the base case:
		nullfall = ScenarioZielnetzBahn.createNullfall1() ;

		// create the policy case:
		planfall = ScenarioZielnetzBahn.createPlanfallBahnausbau(nullfall);

//		runBVWP2003();
//		runBVWP2010();
//		runRoH();
		runBVWP2015();
		
		System.out.println(divider);
		System.out.println(divider);
		
//		planfall = ScenarioZielnetzBahn.createPlanfallStrassenausbau(nullfall) ;
//		runBVWP2003();
//		runBVWP2010();
//		runRoH();
//		runBVWP2015();
	}

//	private static void runRoH() {
//		System.out.println("\n==============================================================================\n" ) ;
//
//		UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();
//		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ;		
//	}
//
	private static void runBVWP2003() {
		System.out.println(divider) ;
		UtilityChanges utilityChanges = new UtilityChangesBVWP2003();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ;
	}
//
//	private static void runBVWP2010() {
//		System.out.println("\n==================================================================================================================================\n" +
//				"Folgende Rechnung versucht die Methodik der Bedarfsplanüberprüfung Schiene 2010 zu nachzuvollziehen. ");
//		
//		UtilityChanges utilityChanges = new UtilityChangesBVWP2010();
//		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall);
//	}

	private static void runBVWP2015() {
		System.out.println(divider);
		UtilityChanges utilityChanges = new UtilityChangesBVWP2015();
		utilityChanges.computeAndPrintResults(economicValues, nullfall, planfall) ;
	}

}
