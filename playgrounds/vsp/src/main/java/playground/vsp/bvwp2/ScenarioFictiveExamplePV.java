package playground.vsp.bvwp2;

import junit.framework.Assert;

import org.matsim.core.basic.v01.IdImpl;

import playground.vsp.bvwp2.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp2.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp2.MultiDimensionalArray.Mode;
import static playground.vsp.bvwp2.Key.*;


/**
 * @author Ihab, Kai
 *
 */

class ScenarioFictiveExamplePV { // Relationsbezogen_mit_generalisierten_Kosten

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation(new IdImpl("BC"), nullfallForOD ) ;
		{
			// construct values for the road mode for this OD relation:
			Mode mode = Mode.road ;
			{
				// passenger traffic:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				nullfallForOD.put(makeKey(mode, segm, Attribute.XX), 3000. ) ;
				nullfallForOD.put(makeKey(mode, segm, Attribute.km), 38. ) ;
				nullfallForOD.put(makeKey(mode, segm, Attribute.hrs), 0.45 ) ;
			}			
		}
		{
			// construct values for the rail mode for this OD relation:
			Mode mode = Mode.rail ;
			{
				// passenger traffic:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				nullfallForOD.put(makeKey(mode, segm, Attribute.XX), 2000. ) ;
				nullfallForOD.put(makeKey(mode, segm, Attribute.km), 41. ) ;
				nullfallForOD.put(makeKey(mode, segm, Attribute.hrs), 0.43 ) ;
				// (yyyyyy: I think this should be _larger_ than for road for a convincing example.  kai, feb'12) 
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
		Values planfallValuesForOD = planfall.getByODRelation(new IdImpl("BC")) ;
		Assert.assertNotNull(planfallValuesForOD) ;
		{
			// modify the travel times for the rail mode:
			DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
			planfallValuesForOD.inc( makeKey( Mode.rail, segm, Attribute.hrs), -0.1 ) ;
			
			// modify some demand (presumably as a result):
			double delta = 100. ;
			planfallValuesForOD.inc( makeKey( Mode.rail, segm, Attribute.XX), delta ) ;
			planfallValuesForOD.inc( makeKey( Mode.road, segm, Attribute.XX), -delta ) ;
		}
		return planfall;
	}

}

