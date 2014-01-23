package playground.vsp.bvwp;

import junit.framework.Assert;

import org.matsim.core.basic.v01.IdImpl;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;
import static playground.vsp.bvwp.Key.*;


/**
 * @author jbischoff
 *
 

mit
Wolfsburg            Röbel                 310301 1305603    349.53    152.22 =2.537

ohne
Wolfsburg            Röbel                 310301 1305603    320.77    165.15 =2.7525

P2030_2010_verbleibend_ME2.csv
  310301; 1305603;           0;           0;           0;          81;        4209;
P2030_2010_neuentstanden_ME2.csv
  310301; 1305603;           0;           0;           0;           8;         545;
P2030_2010_entfallend_ME2.csv
-
P2030_2010_BMVBS_ME2_131008.csv:
310301;1305603
beruf,ausb,einkauf,gesch,url,priv
Bahn
0;0;0;0;0;2;
PKW
0;0;0;118;81;4209;
Luft
0;0;0;0;0;0;
Bus
0;0;0;0;0;1;
Rad
0;0;0;0;0;0;
Fuß
0;0;0;0;0;0
P2030_2010_A14_verlagert_ME2.wid
-
P2030_2010_A14_induz_ME2.wid
# Widerstände MIV im Ohne- und Mit-Projekt-Fall
# von Micro2, nach Micro2,
# ... Reiseweite (Ohne-fall, Mit-Fall) [km],
# ... Reisezeit (Ohne-(06,08), Mit-Fall(07,09)) [Min],
# ... Fahrzeugvorhaltungskosten und -Betriebskosten (Ohne-(10,12),Mit-Fall(11,13)) [/Pkw-Fahrt],
# ... Nutzerkosten je Reisezweck (Beruf, Ausbildung, Einkauf, Geschäft, Urlaub, Privat) (Ohne-(14),Mit-Fall(15)) [/Pers-Fahrt]
# 
  310301; 1305603;    188.96;    217.73;    206.53;    193.60;     37.58;     42.39;     19.91;     16.84;     12.16;     23.34;      9.52;     10.95;     22.52;     19.06;    13.76;     26.48;     10.77;     12.39;
 */

class ScenarioZielnetzRoad {

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;

		//verbleibend
		{
			final Values nullfallForOld = new Values() ;
			nullfall.setValuesForODRelation( new IdImpl("verbleibend"), nullfallForOld ) ;
			
			{
			
				Mode mode = Mode.ROAD ;
				final double travelTime = 206.53/60.; 
				//aus P2030_2010_A14_induz_ME2.wid
				//XX aus P2030_2010_BMVBS_ME2_131008.csv:
				//priceUser aus P2030_2010_A14_induz_ME2.wid
				{
					DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 118. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs),travelTime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), 23.34) ;
		//			nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //????
				}
				
				{
					DemandSegment segm = DemandSegment.PV_BERUF ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs), travelTime) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), 19.91) ;
//					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // ????
					
				}
				{
					DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs),travelTime) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), 16.84) ;
//					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // ????
					
				}
				{
					DemandSegment segm = DemandSegment.PV_EINKAUF ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs),travelTime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), 12.16) ;
//					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // ????
					
				}
				{
					DemandSegment segm = DemandSegment.PV_URLAUB ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 81. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), 9.52) ;
//					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // ????
					
				}
				{
					DemandSegment segm = DemandSegment.PV_SONST ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX), 4209. ) ; 
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser), 10.95) ;
//					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; // ????
					
				}
				
			}
		}

		
		// induced traffic:
		//induziert=entstanden-entfallend
		{
			final Values nullfallForInduced = new Values() ;
			nullfall.setValuesForODRelation(new IdImpl("induziert"), nullfallForInduced ) ;
			{
				{
					Mode mode = Mode.ROAD ;
					final double travelTime = 206.53/60.; 

					{
						//XX = 0, weil im Nullfall ja nichts induziert?
						// commercial passenger traffic:
						DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), 23.34) ;
//						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
					}
					
					{
						DemandSegment segm = DemandSegment.PV_BERUF ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), 19.91) ;
//						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
					}
					
					{
						DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), 16.84) ;
//						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
					}
					{
						DemandSegment segm = DemandSegment.PV_EINKAUF ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), 12.16) ;
//						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
					}
					{
						DemandSegment segm = DemandSegment.PV_URLAUB ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), 9.52) ;
//						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
					}
					{
						DemandSegment segm = DemandSegment.PV_SONST ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
						nullfallForInduced.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
						nullfallForInduced.put(makeKey(mode, segm, Attribute.priceUser), 10.95) ;
//						nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
					}
				
				
				}
				
			}
		}

		// verlagert:
		{
			final Values nullfallForSwitched = new Values() ;
			nullfall.setValuesForODRelation( new IdImpl("verlagert"), nullfallForSwitched ) ;
			//XX = 0 , weil Nullfall
			{	
				Mode mode = Mode.ROAD ;
				final double travelTime = 206.53/60.; 
				{
					DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), 23.34) ;
//					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), distance * 0.1 ) ;???
				}
				{
					DemandSegment segm = DemandSegment.PV_BERUF ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), 19.91) ;
//					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
				}
				
				{
					DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), 16.84) ;
//					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
				}
				{
					DemandSegment segm = DemandSegment.PV_EINKAUF ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), 12.16) ;
//					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
				}
				{
					DemandSegment segm = DemandSegment.PV_URLAUB ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), 9.52) ;
//					nullfallForSwitched.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
				}
				{
					DemandSegment segm = DemandSegment.PV_SONST ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.XX), 0. ) ; 
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.hrs), travelTime ) ;
					nullfallForSwitched.put(makeKey(mode, segm, Attribute.priceUser), 10.95) ;
//					nullfallForInduced.put( makeKey( mode, segm, Attribute.costOfProduction ), 0. ) ; //???
				}
			}
		}
		

		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfallStrassenausbau(ScenarioForEvalData nullfall) {
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		final double tt_diff = 193.60-206.53;

		{
			Values planfallValuesForOD = planfall.getByODRelation(new IdImpl("induziert")) ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ; //Wirtschaftsverkehr bleibt konstant

			}
			{
				DemandSegment segm = DemandSegment.PV_BERUF ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_EINKAUF ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_URLAUB ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 8. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_SONST ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 545. ) ;

			}
			
		}
		{
			Values planfallValuesForOD = planfall.getByODRelation(new IdImpl("verlagert")) ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ; 

			}
			{
				DemandSegment segm = DemandSegment.PV_BERUF ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_EINKAUF ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_URLAUB ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
			{
				DemandSegment segm = DemandSegment.PV_SONST ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;

			}
		}
		{
			Values planfallValuesForOD = planfall.getByODRelation(new IdImpl("verbleibend")) ;
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 

			}
			{
				DemandSegment segm = DemandSegment.PV_BERUF ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 

			}
			{
				DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 

			}
			{
				DemandSegment segm = DemandSegment.PV_EINKAUF ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 

			}
			{
				DemandSegment segm = DemandSegment.PV_URLAUB ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 

			}
			{
				DemandSegment segm = DemandSegment.PV_SONST ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 

			}
		}

		return planfall;
	}

}

