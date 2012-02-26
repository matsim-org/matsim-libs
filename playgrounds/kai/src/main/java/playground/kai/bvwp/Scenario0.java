package playground.kai.bvwp;

import org.matsim.core.basic.v01.IdImpl;

import playground.kai.bvwp.Values.Entry;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.Type;

class Scenario0 {

	static ScenarioForEval createNullfall1() {
		// set up the base case:
		ScenarioForEval nullfall = new ScenarioForEval() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(new IdImpl("AB"), nullfallForOD ) ;
		{
			// construct values for the road mode for this OD relation:
			ValuesForAMode roadValues = nullfallForOD.getByMode(Mode.road) ;
			{
//				// passenger traffic:
//				ValuesForAUserType pvValues = roadValues.getByType(Type.PV) ;
//				pvValues.setByEntry( Entry.amount, 1000. ) ; // number of persons
//				pvValues.setByEntry( Entry.km, 10. ) ;
//				pvValues.setByEntry( Entry.hrs, 1. ) ;
			}
			{
				// freight traffic:
				ValuesForAUserType gvValues = roadValues.getByType(Type.GV) ;
				gvValues.setByEntry( Entry.XX, 1000. ) ; // tons
				gvValues.setByEntry( Entry.km, 10. ) ;
				gvValues.setByEntry( Entry.hrs, 1. ) ;
			}				
			
			// rail values are just a copy of the road values:
			ValuesForAMode railValues = roadValues.createDeepCopy() ;
			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEval createPlanfall1(ScenarioForEval nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEval planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation(new IdImpl("AB")) ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByType(Type.PV_NON_COMMERCIAL).incByEntry( Entry.hrs, -0.1 ) ;
			railValues.getByType(Type.GV).incByEntry( Entry.hrs, -0.1 ) ;
			
			// modify some demand (presumably as a result):
			double delta = 100. ;
//			double delta = 0. ;
			railValues.getByType(Type.GV).incByEntry( Entry.XX, delta ) ;
			planfall.getByODRelation(new IdImpl("AB")).getByMode(Mode.road).getByType(Type.GV).incByEntry(Entry.XX, -delta ) ;
		}
		return planfall;
	}

}
