package playground.vsp.zzArchive.bvwpOld;

import playground.vsp.zzArchive.bvwpOld.Values.Attribute;
import playground.vsp.zzArchive.bvwpOld.Values.DemandSegment;
import playground.vsp.zzArchive.bvwpOld.Values.Mode;

/**
 * @author Ihab
 *
 */

class ScenarioAP200PV { // Relationsbezogen_mit_generalisierten_Kosten

	static ScenarioForEvalData createNullfall() {
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
				Attributes pvValuesRoad = roadValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
				pvValuesRoad.setByEntry( Attribute.XX, 1000. ) ; // number of persons
				pvValuesRoad.setByEntry( Attribute.km, 100. ) ;
				pvValuesRoad.setByEntry( Attribute.hrs, 1. ) ;
			}			
			
			// construct values for the rail mode for this OD relation:
			ValuesForAMode railValues = nullfallForOD.getByMode(Mode.rail) ;
			{
				// passenger traffic:
				Attributes pvValuesRail = railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL) ;
				pvValuesRail.setByEntry( Attribute.XX, 10. ) ; // number of persons
				pvValuesRail.setByEntry( Attribute.km, 100. ) ;
				pvValuesRail.setByEntry( Attribute.hrs, 6. ) ;
			}			
			
//			
//			// rail values are just a copy of the road values:
//			ValuesForAMode railValues = roadValues.createDeepCopy() ;
//			nullfallForOD.setValuesForMode( Mode.rail, railValues ) ;
		}
		
		// return the base case:
		return nullfall;
	}

	/**
	 * "ModeSwitch" means that all new demand comes from some other mode
	 */
	static ScenarioForEvalData createPlanfallModeSwitch(ScenarioForEvalData nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation("BC") ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry( Attribute.hrs, -4. ) ;
			
			// modify some demand (presumably as a result):
			double delta = 90. ;
			railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry( Attribute.XX, delta ) ;
			planfall.getByODRelation("BC").getByMode(Mode.road).getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry(Attribute.XX, -delta ) ;
		}
		return planfall;
	}

	/**
	 * "Induced" means that all new demand comes out of nowhere 
	 */
	static ScenarioForEvalData createPlanfallInduced(ScenarioForEvalData nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation("BC") ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry( Attribute.hrs, -4. ) ;
			
			// modify some demand (presumably as a result):
			double delta = 90. ;
			railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry( Attribute.XX, delta ) ;
			planfall.getByODRelation("BC").getByMode(Mode.road).getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry(Attribute.XX, -0. ) ;
		}
		return planfall;
	}

	/**
	 * "Mixed" means that half of the new demand comes from another mode, and the other half comes out of nowhere
	 */
	static ScenarioForEvalData createPlanfallMixed(ScenarioForEvalData nullfall) {
		// (construct the policy case.  The base case can be used to simplify things ...)
		
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallForOD = planfall.getByODRelation("BC") ;
		{
			// modify the travel times for the rail mode:
			ValuesForAMode railValues = planfallForOD.getByMode( Mode.rail ) ;
			railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry( Attribute.hrs, -4. ) ;
			
			// modify some demand (presumably as a result):
			double delta = 90. ;
			railValues.getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry( Attribute.XX, delta ) ;
			planfall.getByODRelation("BC").getByMode(Mode.road).getByDemandSegment(DemandSegment.PV_NON_COMMERCIAL).incByEntry(Attribute.XX, -delta / 2 ) ;
		}
		return planfall;
	}

}

