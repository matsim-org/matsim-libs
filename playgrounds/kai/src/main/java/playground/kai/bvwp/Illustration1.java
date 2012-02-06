package playground.kai.bvwp;


public class Illustration1 {
	
	public static void main(String[] args) {

		run1();
		
		run2() ;

	}

	private static void run1() {
		// create the economic values
		Values economicValues = Illustration1EconomicValues.createEconomicValues1();
		
		// create the base case:
		ScenarioForEval nullfall = Illustration1Scenario.createNullfall1();
		
		// create the policy case:
		ScenarioForEval planfall = Illustration1Scenario.createPlanfall1(nullfall);
		
		{
			// instantiate the class that defines the utility calculation:
			UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();

			// compute the utilities (currently, results are printed to the console):
			utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
			System.out.println("Without a value of time for freight there are not benefits for rail acceleration measures, despite " +
			"the fact that it is cheaper per km.") ;
		}
		{
			UtilityChanges utilityChanges = new UtilityChangesBVWP2003();
			utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
			System.out.println("As was to be expected, the benefit for the rail acceleration measure derives from the fact that km " +
			"are cheaper by rail than by road.\nIn contrast, there are again no direct time gains.") ;
		}
	}

	private static void run2() {
		// create the economic values
		Values economicValues = Illustration1EconomicValues.createEconomicValues1();
		
		// create the base case:
		ScenarioForEval nullfall = Illustration1Scenario.createNullfall1();
		
		// create the policy case:
		ScenarioForEval planfall = Illustration1Scenario.createPlanfall1(nullfall);
		
		{
			// instantiate the class that defines the utility calculation:
			UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();

			// compute the utilities (currently, results are printed to the console):
			utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
			System.out.println("Without a value of time for freight there are not benefits for rail acceleration measures, despite " +
			"the fact that it is cheaper per km.") ;
		}
		{
			UtilityChanges utilityChanges = new UtilityChangesBVWP2003();
			utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
			System.out.println("As was to be expected, the benefit for the rail acceleration measure derives from the fact that km " +
			"are cheaper by rail than by road.\nIn contrast, there are again no direct time gains.") ;
		}
	}

}
