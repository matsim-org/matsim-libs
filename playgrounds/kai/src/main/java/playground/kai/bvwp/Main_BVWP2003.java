package playground.kai.bvwp;


public class Main_BVWP2003 {

	public static void main(String[] args) {
		
		// create the economic values
		Values economicValues = EconomicValues4.createEconomicValues1();
		System.out.println("\n===\nFolgende Rechnung entspricht exakt ``rechnungen>javaRechnungen>BVWP2003.xlsx'' (EconomicValues, Scenario und Methodik) ");

		// create the base case:
		ScenarioForEval nullfall = Scenario4.createNullfall1();
		
		// create the policy case:
		ScenarioForEval planfall = Scenario4.createPlanfall1(nullfall);
		
		// instantiate the class that defines the utility calculation:
		UtilityChanges utilityChanges = new UtilityChangesBVWP2003();

		// compute the utilities (currently, results are printed to the console):
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
		
	}

}
