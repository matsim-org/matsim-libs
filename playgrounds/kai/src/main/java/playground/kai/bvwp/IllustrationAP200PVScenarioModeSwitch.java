package playground.kai.bvwp;

import org.matsim.core.basic.v01.IdImpl;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

/**
 * @author Ihab
 *
 */

class IllustrationAP200PVScenarioModeSwitch { // Relationsbezogen_mit_generalisierten_Kosten

	static ScenarioForEval createNullfall1() {
		// set up the base case:
		ScenarioForEval nullfall = new ScenarioForEval() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(new IdImpl("BC"), nullfallForOD ) ;
		{
			// construct values for the road mode for this OD relation:
			ValuesForAMode roadValues = nullfallForOD.getByMode(Mode.road) ;
			{
				// passenger traffic:
				ValuesForAUserType pvValuesRoad = roadValues.getByType(Type.PV_NON_COMMERCIAL) ;
				pvValuesRoad.setByEntry( Entry.XX, 1000. ) ; // number of persons
				pvValuesRoad.setByEntry( Entry.km, 100. ) ;
				pvValuesRoad.setByEntry( Entry.hrs, 1. ) ;
			}			
			
			// construct values for the rail mode for this OD relation:
			ValuesForAMode railValues = nullfallForOD.getByMode(Mode.rail) ;
			{
				// passenger traffic:
				ValuesForAUserType pvValuesRail = railValues.getByType(Type.PV_NON_COMMERCIAL) ;
				pvValuesRail.setByEntry( Entry.XX, 10. ) ; // number of persons
				pvValuesRail.setByEntry( Entry.km, 100. ) ;
				pvValuesRail.setByEntry( Entry.hrs, 6. ) ;
			}			
			
//			
//			// rail values are just a copy of the road values:
//			ValuesForAMode railValues = roadValues.createDeepCopy() ;
//			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEval createPlanfall1(ScenarioForEval nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEval planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation(new IdImpl("BC")) ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByType(Type.PV_NON_COMMERCIAL).incByEntry( Entry.hrs, -4. ) ;
			
			// modify some demand (presumably as a result):
			double delta = 90. ;
			railValues.getByType(Type.PV_NON_COMMERCIAL).incByEntry( Entry.XX, delta ) ;
			planfall.getByODRelation(new IdImpl("BC")).getByMode(Mode.road).getByType(Type.PV_NON_COMMERCIAL).incByEntry(Entry.XX, -delta ) ;
		}
		return planfall;
	}

}

