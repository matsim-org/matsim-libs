package playground.vsp.bvwp;

import junit.framework.Assert;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.ChangeType;
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
		Id oDRelation = new IdImpl("Wolfsburg-Roebel");
		final double travelTime = 165.15/60.; 
		final double belegungsgrad_PV_SONST = 1.3;

		final Values nullfallForOld = new Values() ;
		nullfall.setValuesForODRelation( oDRelation, nullfallForOld ) ;
		for (ChangeType type : ChangeType.values())
		{
		//verbleibend
		{

			
			{
			
				Mode mode = Mode.ROAD ;
				//Widerstände aus grosser Tabelle
				
				//priceUser + cost of production aus P2030_2010_A14_induz_ME2.wid
//				{
//					DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.XX, type), 118. ) ; 
//					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs, type),travelTime ) ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser, type), 23.34) ;
//		//			nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction , type), 0. ) ; //????
//				}
//				
//				{
//					DemandSegment segm = DemandSegment.PV_BERUF ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.XX, type), 0. ) ; 
//					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs, type), travelTime) ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser, type), 19.91) ;
////					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction , type), 0. ) ; // ????
//					
//				}
//				{
//					DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.XX, type), 0. ) ; 
//					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs, type),travelTime) ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser, type), 16.84) ;
////					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction , type), 0. ) ; // ????
//					
//				}
//				{
//					DemandSegment segm = DemandSegment.PV_EINKAUF ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.XX, type), 0. ) ; 
//					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs, type),travelTime ) ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser, type), 12.16) ;
////					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction , type), 0. ) ; // ????
//					
//				}
//				{
//					DemandSegment segm = DemandSegment.PV_URLAUB ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.XX, type), 81. ) ; 
//					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs, type), travelTime ) ;
//					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser, type), 9.52) ;
////					nullfallForOld.put( makeKey( mode, segm, Attribute.costOfProduction , type), 0. ) ; // ????
//					
//				}
				{
					DemandSegment segm = DemandSegment.PV_SONST ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX, type), 4209. ) ; 
					System.out.println("created  ct " + type);
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs, type), travelTime ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser, type), 10.95 * belegungsgrad_PV_SONST) ;
					nullfallForOld.put(makeKey( mode, segm, Attribute.costOfProduction , type), 37.58 ) ; 
					
				}
				
				
				
				
			}
			{
				Mode mode = Mode.RAIL ;
				{
					DemandSegment segm = DemandSegment.PV_SONST ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.XX, type), 1000. ) ; 
					System.out.println("created  ct " + type);
					nullfallForOld.put(makeKey(mode, segm, Attribute.hrs, type), 4.25 ) ;
					nullfallForOld.put(makeKey(mode, segm, Attribute.priceUser, type), 35.0) ; //BC50-Preis
					nullfallForOld.put(makeKey( mode, segm, Attribute.costOfProduction , type), 0.0 ) ; 
					
				}
				
			}
		}


			
			
		}
	
	System.out.println(	nullfall.getAllRelations());
		
		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfallStrassenausbau(ScenarioForEvalData nullfall) {
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;
		final double tt_inc = (152.22/60.)-(165.15/60.);
		final double costOfProd_Diff = 42.39-37.58;
		Id oDRelation = new IdImpl("Wolfsburg-Roebel");


		
			Values planfallValuesForOD = planfall.getByODRelation(oDRelation) ;
			Assert.assertNotNull(planfallValuesForOD) ;
//			{
//				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ; //Wirtschaftsverkehr bleibt konstant
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_BERUF ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_EINKAUF ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_URLAUB ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 8. ) ;
//
//			}
			{
				DemandSegment segm = DemandSegment.PV_SONST ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs, ChangeType.INDUZIERT), tt_inc ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX, ChangeType.INDUZIERT), 545. ) ; // neuentstanden-wegfallend
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.priceUser, ChangeType.INDUZIERT), 0. ) ; //nur damit wir es nicht vergessen...
				planfallValuesForOD.inc(makeKey(Mode.ROAD, segm , Attribute.costOfProduction, ChangeType.INDUZIERT),costOfProd_Diff);

			}
			
		 
		
		
		//ab hier: verlagert (Fantasiewerte)
		

//			{
//				DemandSegment segm = DemandSegment.PV_COMMERCIAL ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ; //Wirtschaftsverkehr bleibt konstant
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_BERUF ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_AUSBILDUNG ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_EINKAUF ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 0. ) ;
//
//			}
//			{
//				DemandSegment segm = DemandSegment.PV_URLAUB ;
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs), tt_diff ) ; 
//				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX), 8. ) ;
//
//			}
			{
				DemandSegment segm = DemandSegment.PV_SONST ;
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.hrs, ChangeType.VERLAGERT), -0.5 ) ; 
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.XX, ChangeType.VERLAGERT), 100. ) ; // neuentstanden-wegfallend
				planfallValuesForOD.inc( makeKey( Mode.ROAD, segm, Attribute.priceUser, ChangeType.VERLAGERT), 0. ) ; //nur damit wir es nicht vergessen...
				planfallValuesForOD.inc(makeKey(Mode.ROAD, segm , Attribute.costOfProduction, ChangeType.VERLAGERT),costOfProd_Diff);

			}
			{
				DemandSegment segm = DemandSegment.PV_SONST ;
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.hrs, ChangeType.VERLAGERT), 0.0 ) ; 
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.XX, ChangeType.VERLAGERT), -100. ) ; // wegfallend
				planfallValuesForOD.inc( makeKey( Mode.RAIL, segm, Attribute.priceUser, ChangeType.VERLAGERT), 0. ) ; //nur damit wir es nicht vergessen...
				planfallValuesForOD.inc(makeKey(Mode.RAIL, segm , Attribute.costOfProduction, ChangeType.VERLAGERT),costOfProd_Diff);

			}

		return planfall;
	}

}

