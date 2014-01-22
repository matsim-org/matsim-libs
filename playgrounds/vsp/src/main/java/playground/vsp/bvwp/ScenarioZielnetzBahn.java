package playground.vsp.bvwp;

import junit.framework.Assert;

import org.matsim.core.basic.v01.IdImpl;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;
import static playground.vsp.bvwp.Key.*;


/**
 * @author Kai
 *
 */

class ScenarioZielnetzBahn {

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;

		// induced traffic:
		{
			final Values nullfallForInduced = new Values() ;
			nullfall.setValuesForODRelation(new IdImpl("induziert"), nullfallForInduced ) ;
			{
				{
					Mode mode = Mode.RAIL ;
					{
						// commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
						final double distance = 580. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), 5.5 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
					}
					{
						// non-commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
						final double distance = 620. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), 5.9 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0.  ) ; 
					}
				}
				{
					Mode mode = Mode.ROAD ; // we need a fake road!
					{
						// commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
						final double distance = 580. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), 5.5 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
					}
					{
						// non-commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
						final double distance = 620. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), 5.9 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0.  ) ; 
					}
				}
			}
		}

		// verlagert:
		{
			final Values nullfallForSwitched = new Values() ;
			nullfall.setValuesForODRelation( new IdImpl("verlagert"), nullfallForSwitched ) ;
			{
				Mode mode = Mode.RAIL ;
				{
					DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
					final double distance = 315. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), 4.0 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 210. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), 3.2 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
			}
			{	
				Mode mode = Mode.ROAD ;
				{
					DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
					final double distance = 315. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 2.5*1000.*1000. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), 3.5 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), distance * 0.1 ) ;
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 210. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 6.6*1000.*1000. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), 2.7 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), distance * 0.1 ) ; 
				}
			}
		}
		{
			final Values nullfallForOld = new Values() ;
			nullfall.setValuesForODRelation( new IdImpl("verbleibend"), nullfallForOld ) ;
			{
				Mode mode = Mode.RAIL ;
				{
					DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
					final double distance =  374. ;  // 13788. / 36.8 ;
					final double ttime = distance * 0.15 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 36.8 * 1000. * 1000.  ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 281. ; // 32700. / 116.5 ;
					final double ttime = distance * 0.12 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 116.5 * 1000. * 1000. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
			}
			{
				Mode mode = Mode.ROAD ;
				{
					DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
					final double distance =  374. ;  // 13788. / 36.8 ;
					final double ttime = distance * 0.15 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 281. ; // 32700. / 116.5 ;
					final double ttime = distance * 0.12 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
			}
		}

		// return the base case:
		return nullfall;
	}


	static ScenarioForEvalData createPlanfallBahnausbau(ScenarioForEvalData nullfall) {
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;

		{
			Values planfallValuesForOD = planfall.getByODRelation(new IdImpl("induziert")) ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.hrs), -0.75 ) ; 
				// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.XX), 0.8*1000.*1000. ) ;

			}
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.hrs), -0.58 ) ; 
				// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.XX), 1.2*1000.*1000. ) ;

			}
		}
		{
			Values planfallValuesForOD = planfall.getByODRelation(new IdImpl("verlagert")) ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				double xx_verlagert = 2.5 * 1000. * 1000. ;

				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
				final Key key = makeKey( Mode.RAIL, segm, Attribute.hrs);
				System.err.flush(); 
				System.out.println( planfallValuesForOD.get(key).toString() )  ;
				System.out.flush();
				planfallValuesForOD.inc( key, -0.68 ) ;

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.XX), xx_verlagert ) ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), -xx_verlagert ) ;

			}
			{
				double xx_verlagert = 6.6 * 1000. *1000. ;

				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.hrs), -0.50 ) ;

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.XX), xx_verlagert ) ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), -xx_verlagert ) ;

			}
		}
		{
			Values planfallValuesForOD = planfall.getByODRelation(new IdImpl("verbleibend")) ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.hrs), -0.18 ) ; 

				// no demand modif ("verbleibend")
			}
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.hrs), -0.13 ) ; 

				// no demand modif ("verbleibend")
			}
		}

		return planfall;
	}

}

