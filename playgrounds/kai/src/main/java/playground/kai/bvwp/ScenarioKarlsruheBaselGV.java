package playground.kai.bvwp;

import org.matsim.core.basic.v01.IdImpl;

import playground.kai.bvwp.Values.Attribute;
import playground.kai.bvwp.Values.Mode;
import playground.kai.bvwp.Values.DemandSegment;

class ScenarioKarlsruheBaselGV {

	static final String relation = "Karlsruhe-Basel";

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(new IdImpl(relation), nullfallForOD ) ;
		{
			{
				ValuesForAMode valuesForAMode = nullfallForOD.getByMode(Mode.road) ;
				{
					ValuesForAUserType valuesForModeAndDemandSegment = valuesForAMode.getByDemandSegment(DemandSegment.GV) ;
					valuesForModeAndDemandSegment.setByEntry( Attribute.XX, 2.6e8 ) ; // number of tons.  Irrelevant dummy value.
					valuesForModeAndDemandSegment.setByEntry( Attribute.km, 200. ) ;
					valuesForModeAndDemandSegment.setByEntry( Attribute.hrs, 3. ) ;
					valuesForModeAndDemandSegment.setByEntry( Attribute.priceUser, 200.*0.1 ) ;
					valuesForModeAndDemandSegment.setByEntry( Attribute.costOfProduction, 200.*0.1 ) ;
				}
			}
			{
				ValuesForAMode valuesForAMode = nullfallForOD.getByMode(Mode.rail) ;
				{
					ValuesForAUserType valuesForModeAndDemandSegment = valuesForAMode.getByDemandSegment(DemandSegment.GV) ;
					valuesForModeAndDemandSegment.setByEntry( Attribute.XX, 2.6e7 ) ; // number of tons
					valuesForModeAndDemandSegment.setByEntry( Attribute.km, 200. ) ;
					valuesForModeAndDemandSegment.setByEntry( Attribute.hrs, 10. ) ; 
					valuesForModeAndDemandSegment.setByEntry( Attribute.priceUser, 200.*0.1 ) ;
					valuesForModeAndDemandSegment.setByEntry( Attribute.costOfProduction, 200.*0.1 ) ;
				}
			}
			
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfall1(ScenarioForEvalData nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation(new IdImpl(relation)) ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode valuesForAMode = planfallForOD.getByMode( Mode.rail ) ;
			valuesForAMode.getByDemandSegment(DemandSegment.GV).incByEntry( Attribute.hrs, -6. ) ;
			
			// modify some demand (presumably as a result):
			double delta = 0. ;
			valuesForAMode.getByDemandSegment(DemandSegment.GV).incByEntry( Attribute.XX, delta ) ;
			planfall.getByODRelation(new IdImpl(relation)).getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).incByEntry(Attribute.XX, -delta ) ;
		}
		return planfall;
	}

}

