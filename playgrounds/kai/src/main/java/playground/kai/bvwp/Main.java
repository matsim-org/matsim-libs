package playground.kai.bvwp;


public class Main {

	public static void main(String[] args) {

		// create the economic values
		Values economicValues = EconomicValues1.createEconomicValues1();
		
		// create the base case:
		ScenarioForEval nullfall = Scenario1.createNullfall1();
		
		// create the policy case:
		ScenarioForEval planfall = Scenario1.createPlanfall1(nullfall);
		
		// instantiate the class that defines the utility calculation:
		UtilityChangesNEW utilityChanges = new UtilityChangesNEW();
		
		// compute the utilities (currently, results are printed to the console):
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
		
	}

}
