package playground.kai.bvwp;

import org.matsim.core.basic.v01.IdImpl;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;


public class IllustrationFictiveExamplePVwithoutOldUsers {

	private static Values economicValues;
	private static ScenarioForEvalData nullfall;
	private static ScenarioForEvalData planfall;

	public static void main(String[] args) {

		// create the economic values
		economicValues = EconomicValues.createEconomicValuesFictiveExamplePV();

		// create the base case:
		nullfall = ScenarioFictiveExamplePV.createNullfall1();
		Values nullfallForOD = nullfall.getByODRelation(new IdImpl("BC")) ;
		
		// construct values for the rail mode for this OD relation:
		ValuesForAMode railValues = nullfallForOD.getByMode(Mode.rail) ;
		{
			// passenger traffic:
			ValuesForAUserType pvValuesRail = railValues.getByType(Type.PV_NON_COMMERCIAL) ;
			pvValuesRail.setByEntry( Entry.XX, 0. ) ; // number of persons
		}			


		// create the policy case:
		planfall = ScenarioFictiveExamplePV.createPlanfall1(nullfall);

		runBVWP2003();
		runBVWP2010();
		runRoH();
		runBVWP2015();
	}

	private static void runRoH() {
		System.out.println("\n==================================================================================================================================\n" +
				"Folgende Rechnung entspricht exakt ``rechnungen>javaRechnungen>RoH_PV.xlsx'' (EconomicValues, Scenario und Methodik) ");

		UtilityChanges utilityChanges = new UtilityChangesRuleOfHalf();
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;		
	}

	private static void runBVWP2003() {
		System.out.println("\n==================================================================================================================================\n" +
				"Folgende Rechnung entspricht exakt ``rechnungen>javaRechnungen>BVWP2003.xlsx'' (EconomicValues, Scenario und Methodik) ");

		UtilityChanges utilityChanges = new UtilityChangesBVWP2003();
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
	}

	private static void runBVWP2010() {
		System.out.println("\n==================================================================================================================================\n" +
				"Folgende Rechnung versucht die Methodik der Bedarfsplanüberprüfung Schiene 2010 zu nachzuvollziehen. ");
		
		UtilityChanges utilityChanges = new UtilityChangesBVWP2010();
		utilityChanges.utilityChange(economicValues, nullfall, planfall);
	}

	private static void runBVWP2015() {
		System.out.println("\n==================================================================================================================================\n" +
				"Folgende Rechnung ergibt das gleiche Ergebnis wie RoH.");
		UtilityChanges utilityChanges = new UtilityChangesBVWP2015();
		utilityChanges.utilityChange(economicValues, nullfall, planfall) ;
	}

}
