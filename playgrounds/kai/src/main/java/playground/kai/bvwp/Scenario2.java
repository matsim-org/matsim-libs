package playground.kai.bvwp;

import org.matsim.core.basic.v01.IdImpl;
import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

/**
 * @author Ihab
 *
 */

class Scenario2 { // Relationsbezogen_mit_generalisierten_Kosten

	static ScenarioForEvalData createNullfall() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(new IdImpl("BC"), nullfallForOD ) ;
		{
			// construct values for the road mode for this OD relation:
			ValuesForAMode roadValues = nullfallForOD.getByMode(Mode.road) ;
			{
				// passenger traffic:
				ValuesForAUserType pvValues = roadValues.getByType(Type.PV_NON_COMMERCIAL) ;
				pvValues.setByEntry( Entry.XX, 2000. ) ; // number of persons
				pvValues.setByEntry( Entry.km, 41. ) ;
				pvValues.setByEntry( Entry.hrs, 0.43 ) ;
			}				
			
			// rail values are just a copy of the road values:
			ValuesForAMode railValues = roadValues.createDeepCopy() ;
			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfall(ScenarioForEvalData nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation(new IdImpl("BC")) ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByType(Type.PV_NON_COMMERCIAL).incByEntry( Entry.hrs, -0.08 ) ;
			
			// modify some demand (presumably as a result):
			double delta = 100. ;
			railValues.getByType(Type.PV_NON_COMMERCIAL).incByEntry( Entry.XX, delta ) ;
			planfall.getByODRelation(new IdImpl("BC")).getByMode(Mode.road).getByType(Type.PV_NON_COMMERCIAL).incByEntry(Entry.XX, -delta ) ;
		}
		return planfall;
	}

}

