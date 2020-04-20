package playground.vsp.bvwp;

import static playground.vsp.bvwp.Key.makeKey;
import junit.framework.Assert;
import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;


/**
 * @author Kai
 *
 */

class ScenarioZielnetzBahn2 {
	
	static final double xx_bahn = 116.5 * 1000. * 1000.  ;
	static final double pkm_bahn = 32700. * 1000. * 1000. ;
	static final double distance = pkm_bahn / xx_bahn ;
	
	static final double xx_bahn_comm = 36.8 * 1000. * 1000. ;

	static final double verbleibend_bahn =  xx_bahn ;
	static final double verbleibend_strasse = xx_bahn * 10. ;
	 static final double verbleibend_bahn_comm = xx_bahn_comm ; 
	 static final double verbleibend_strasse_comm = xx_bahn_comm * 5. ;
//	static final double verbleibend_bahn =  0. ;
//	static final double verbleibend_strasse = 0. ;
//	static final double verbleibend_bahn_comm = 0. ;
//	static final double verbleibend_strasse_comm = 0. ;
	
	static final double xxi_bahn = 1.2 * 1000. * 1000. ;
	static final double xxi_bahn_comm = 0.8 * 1000. * 1000. ;

	static final double induziert_bahn = xxi_bahn ;
	static final double induziert_strasse = xxi_bahn * 10. ;
	static final double induziert_bahn_comm =  xxi_bahn_comm ;
	static final double induziert_strasse_comm = xxi_bahn_comm * 5. ;
//	static final double induziert_bahn = 0. ;
//	static final double induziert_strasse = 0. ;
//	static final double induziert_bahn_comm = 0. ;
//	static final double induziert_strasse_comm = 0. ;
	
	static final double xxv_bahn = 6.6 * 1000. * 1000. ;
	static final double xxv_bahn_comm = 2.5 * 1000. * 1000. ;

	static final double verlagert_bahn_von_strasse =  xxv_bahn ;
	static final double verlagert_strasse_von_bahn = xxv_bahn / 10. ;
	static final double verlagert_bahn_von_strasse_comm = xxv_bahn_comm ;
	static final double verlagert_strasse_von_bahn_comm = xxv_bahn_comm / 5. ;
//	static final double verlagert_bahn_von_strasse = 0. ;
//	static final double verlagert_strasse_von_bahn = 0. ;
//	 static final double verlagert_bahn_von_strasse_comm = 0. ;
//	 static final double verlagert_strasse_von_bahn_comm = 0. ;



	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;
	
		// construct values for one OD relation:
		Values nullfallForOD = new Values() ;
		nullfall.setValuesForODRelation("BC", nullfallForOD ) ;
		{
			System.out.println(" distance: " + distance );
			{
				// construct values for the road mode for this OD relation:
				Mode mode = Mode.Strasse ;
				{
					// non-commercial passenger traffic:
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.XX), verbleibend_strasse ) ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.Reisezeit_h), 2.5 ) ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOD.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), (0.1 - 0.02) * distance  ) ; // price minus taxes
					// this is per demand item!!!
				}			
				{
					// commercial passenger traffic:
					DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.XX), verbleibend_strasse_comm ) ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.Reisezeit_h), 2.5 ) ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOD.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), (0.1 - 0.02) * distance  ) ; // price minus taxes
					// this is per demand item!!!
				}			

			}
			{
				// construct values for the RAIL mode for this OD relation:
				Mode mode = Mode.Bahn ;
				{
					// non-commercial passenger traffic:
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.XX), verbleibend_bahn ) ; 
					nullfallForOD.put(makeKey(mode, segm, Attribute.Reisezeit_h), 3. ) ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOD.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
				}			
				{
					// commercial passenger traffic:
					DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.XX), verbleibend_bahn_comm ) ; 
					nullfallForOD.put(makeKey(mode, segm, Attribute.Reisezeit_h), 3. ) ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOD.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
			}
		}
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfallBahnausbau(ScenarioForEvalData nullfall) {
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallValuesForOD = planfall.getByODRelation("BC") ;
		Assert.assertNotNull(planfallValuesForOD) ;
		{
			// modify the travel times for the rail mode:
			DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
			planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.Reisezeit_h), -0.13 ) ; // ausgedacht
			
			// modify some demand (presumably as a result):
			planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), verlagert_bahn_von_strasse + induziert_bahn) ;
			planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.XX), -verlagert_bahn_von_strasse ) ;
			
		}
//		{
//			// modify the travel times for the rail mode:
//			DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
//			planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.hrs), -0.13 ) ; // ausgedacht
//			
//			// modify some demand (presumably as a result):
//			planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.XX), verlagert_bahn_von_strasse_comm + induziert_bahn_comm) ;
//			planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), -verlagert_bahn_von_strasse_comm ) ;
//			
//		}
		return planfall;
	}

	static ScenarioForEvalData createPlanfallStrassenausbau(ScenarioForEvalData nullfall) {
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		
		// we are now looking at one specific OD relation (for this scenario, there is only one!)
		Values planfallValuesForOD = planfall.getByODRelation("BC") ;
		Assert.assertNotNull(planfallValuesForOD) ;
		{
			// modify the travel times for the rail mode:
			DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
			planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.Reisezeit_h), -0.13 ) ; // ausgedacht
			
			// modify some demand (presumably as a result):
			planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.XX), verlagert_strasse_von_bahn + induziert_strasse) ;
			planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), -verlagert_strasse_von_bahn ) ;
			
		}
		{
			// modify the travel times for the rail mode:
			DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
			planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.Reisezeit_h), -0.13 ) ; // ausgedacht
			
			// modify some demand (presumably as a result):
			planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.XX), verlagert_strasse_von_bahn_comm + induziert_strasse_comm) ;
			planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), -verlagert_strasse_von_bahn_comm ) ;
			
		}
		return planfall;
	}

}

