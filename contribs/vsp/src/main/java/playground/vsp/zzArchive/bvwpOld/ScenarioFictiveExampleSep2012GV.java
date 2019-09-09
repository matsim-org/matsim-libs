package playground.vsp.zzArchive.bvwpOld;

import playground.vsp.zzArchive.bvwpOld.Values.Attribute;
import playground.vsp.zzArchive.bvwpOld.Values.DemandSegment;
import playground.vsp.zzArchive.bvwpOld.Values.Mode;

/**
 * @author Ihab
 *
 */

class ScenarioFictiveExampleSep2012GV { // Relationsbezogen_mit_generalisierten_Kosten

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation("BC", nullfallForOD ) ;
		{
			// construct values for the road mode for this OD relation:
			ValuesForAMode roadValues = nullfallForOD.getByMode(Mode.road) ;
			{
				// passenger traffic:
				Attributes valuesRoad = roadValues.getByDemandSegment(DemandSegment.GV) ;
				valuesRoad.setByEntry( Attribute.XX, 3000. ) ; // number of persons
				valuesRoad.setByEntry( Attribute.km, 38. ) ;
				valuesRoad.setByEntry( Attribute.hrs, 0.3 ) ;
			}			
			
			// construct values for the rail mode for this OD relation:
			ValuesForAMode railValues = nullfallForOD.getByMode(Mode.rail) ;
			{
				// passenger traffic:
				Attributes valuesRail = railValues.getByDemandSegment(DemandSegment.GV) ;
				valuesRail.setByEntry( Attribute.XX, 2000. ) ; // number of persons
				valuesRail.setByEntry( Attribute.km, 41. ) ;
				valuesRail.setByEntry( Attribute.hrs, 0.43 ) ; 
				valuesRail.setByEntry( Attribute.priceUser, 41.*0.1 ) ;
				valuesRail.setByEntry( Attribute.costOfProduction, 41.*0.1 ) ;
			}			
			
//			
//			// rail values are just a copy of the road values:
//			ValuesForAMode railValues = roadValues.createDeepCopy() ;
//			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfall1(ScenarioForEvalData nullfall) {
		System.out.println("This is an example where (1) the rail GV travel time is improved; (2) additional demand switches to rail in consequence.") ;
		System.out.println("As long as the VoT is zero, this will NOT lead to benefits according to RoH or with implicit utility.") ;
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation("BC") ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByDemandSegment(DemandSegment.GV).incByEntry( Attribute.hrs, -0.1 ) ;
			
			// modify some demand (presumably as a result):
			double delta = 100. ;
			railValues.getByDemandSegment(DemandSegment.GV).incByEntry( Attribute.XX, delta ) ;
			planfall.getByODRelation("BC").getByMode(Mode.road).getByDemandSegment(DemandSegment.GV).incByEntry(Attribute.XX, -delta ) ;
		}
		return planfall;
	}

}

