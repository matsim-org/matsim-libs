package playground.kai.bvwp;


public class Main {

	public static void main(String[] args) {

		// create the economic values
		Values economicValues = EconomicValues4.createEconomicValues1();
		
		// create the base case:
		ScenarioForEval nullfall = Scenario4.createNullfall1();
		
		// create the policy case:
		ScenarioForEval planfall = Scenario4.createPlanfall1(nullfall);
		
		// instantiate the class that defines the utility calculation:
//		UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();
		UtilityChanges utilityChanges = new UtilityChangesBVWP2003();

		// compute the utilities (currently, results are printed to the console):
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
		
	}

}
