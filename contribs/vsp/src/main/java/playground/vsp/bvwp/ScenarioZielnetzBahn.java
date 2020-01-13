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

class ScenarioZielnetzBahn {

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;

		// induced traffic:
		{
			final Values nullfallForInduced = new Values() ;
			nullfall.setValuesForODRelation("induziert", nullfallForInduced ) ;
			{
				{
					Mode mode = Mode.Bahn ;
					{
						// commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
						final double distance = 580. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Reisezeit_h), 5.5 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
					}
					{
						// non-commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
						final double distance = 620. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Reisezeit_h), 5.9 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0.  ) ; 
					}
				}
				{
					Mode mode = Mode.Strasse ; // we need a fake road!
					{
						// commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
						final double distance = 580. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Reisezeit_h), 5.5 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
					}
					{
						// non-commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
						final double distance = 620. ; 

						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Reisezeit_h), 5.9 ) ;
						// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)
						nullfallForInduced.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
						nullfallForInduced.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0.  ) ; 
					}
				}
			}
		}

		// verlagert:
		{
			final Values nullfallForSwitched = new Values() ;
			nullfall.setValuesForODRelation( "verlagert", nullfallForSwitched ) ;
			{
				Mode mode = Mode.Bahn ;
				{
					DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
					final double distance = 315. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Reisezeit_h), 4.0 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 210. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Reisezeit_h), 3.2 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
			}
			{	
				Mode mode = Mode.Strasse ;
				{
					DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
					final double distance = 315. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 2.5*1000.*1000. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Reisezeit_h), 3.5 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), distance * 0.1 ) ;
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 210. ;

					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 6.6*1000.*1000. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Reisezeit_h), 2.7 ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForSwitched.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), distance * 0.1 ) ; 
				}
			}
		}
		{
			final Values nullfallForOld = new Values() ;
			nullfall.setValuesForODRelation( "verbleibend", nullfallForOld ) ;
			{
				Mode mode = Mode.Bahn ;
				{
					DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
					final double distance =  374. ;  // 13788. / 36.8 ;
					final double ttime = distance * 0.15 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 36.8 * 1000. * 1000.  ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.Reisezeit_h), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 281. ; // 32700. / 116.5 ;
					final double ttime = distance * 0.12 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 116.5 * 1000. * 1000. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.Reisezeit_h), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
			}
			{
				Mode mode = Mode.Strasse ;
				{
					DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
					final double distance =  374. ;  // 13788. / 36.8 ;
					final double ttime = distance * 0.15 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.Reisezeit_h), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
				}
				{
					DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
					final double distance = 281. ; // 32700. / 116.5 ;
					final double ttime = distance * 0.12 ;

					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.Reisezeit_h), ttime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), distance*0.1) ;
					nullfallForOld.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // assume (marginal) production cost of rail is zero
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
			Values planfallValuesForOD = planfall.getByODRelation("induziert") ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.Reisezeit_h), -0.75 ) ; 
				// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), 0.8*1000.*1000. ) ;

			}
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.Reisezeit_h), -0.58 ) ; 
				// (we need both the base case and the measure case here to be able to compute the "half" improvement!!!)

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), 1.2*1000.*1000. ) ;

			}
		}
		{
			Values planfallValuesForOD = planfall.getByODRelation("verlagert") ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				double xx_verlagert = 2.5 * 1000. * 1000. ;

				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
				final Key key = makeKey( Mode.Bahn, segm, Attribute.Reisezeit_h);
				System.err.flush(); 
				System.out.println( planfallValuesForOD.get(key).toString() )  ;
				System.out.flush();
				planfallValuesForOD.inc( key, -0.68 ) ;

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), xx_verlagert ) ;
				planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.XX), -xx_verlagert ) ;

			}
			{
				double xx_verlagert = 6.6 * 1000. *1000. ;

				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.Reisezeit_h), -0.50 ) ;

				// modify some demand (presumably as a result):
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), xx_verlagert ) ;
				planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.XX), -xx_verlagert ) ;

			}
		}
		{
			Values planfallValuesForOD = planfall.getByODRelation("verbleibend") ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_GESCHAEFT ;
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.Reisezeit_h), -0.18 ) ; 

				// no demand modif ("verbleibend")
			}
			{
				// modify the travel times for the rail mode:
				DemandSegment segm = DemandSegment.PV_NON_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.Reisezeit_h), -0.13 ) ; 

				// no demand modif ("verbleibend")
			}
		}

		return planfall;
	}

}

