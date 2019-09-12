package playground.vsp.bvwp;

import static playground.vsp.bvwp.Key.makeKey;
import junit.framework.Assert;
import playground.vsp.bvwp.MultiDimensionalArray.Attribute;
import playground.vsp.bvwp.MultiDimensionalArray.DemandSegment;
import playground.vsp.bvwp.MultiDimensionalArray.Mode;


/**
 * @author jbischoff
 *
 

mit
Magdeburg Mitte      Stendal              1500301 1509001     65.35     70.11
ohne
Magdeburg Mitte      Stendal              1500301 1509001     65.72     86.30

P2030_2010_verbleibend_ME2.csv
 1500301; 1509001;       93959;         727;       22026;           0;       10862;
P2030_2010_neuentstanden_ME2.csv
 1500301; 1509001;       57849;        1701;       23204;           0;       15784;
 P2030_2010_entfallend_ME2.csv
---
P2030_2010_BMVBS_ME2_131008.csv:
1500301;1509001
beruf,ausb,einkauf,gesch,url,priv
Bahn
43957;1606;4481;9317;0;8547 =     
PKW
;93959;727;22026;14275;0;10862
Luft
;0;0;0;0;0;0
Bus
;0;0;0;0;0;268
Rad
;0;0;0;0;0;0
Fuß
;0;0;0;0;0;0
P2030_2010_A14_verlagert_ME2.wid
# ... Personenfahrten(SPV) pro Werktag, tij(SPV)[min], tij(MIV(ohne))[min], tij(MIV(mit))[min], SPV-Ant(ohne), SPV-Ant(mit), rel(tij(IV)), Fij(verlagert)
 1500301;Magdeburg Mitte     ; 1509001;Stendal             ;    214.25;     77.80;     79.31;     63.12;      0.14;      0.09;      0.80;     43.73;
 
P2030_2010_A14_induz_ME2.wid
# Widerstände MIV im Ohne- und Mit-Projekt-Fall
# von Micro2, nach Micro2,
# ... Reiseweite (Ohne-fall, Mit-Fall) [km],
# ... Reisezeit (Ohne-(06,08), Mit-Fall(07,09)) [Min],
# ... Fahrzeugvorhaltungskosten und -Betriebskosten (Ohne-(10,12),Mit-Fall(11,13)) [/Pkw-Fahrt],
# ... Nutzerkosten je Reisezweck (Beruf, Ausbildung, Einkauf, Geschäft, Urlaub, Privat) (Ohne-(14),Mit-Fall(15)) [/Pers-Fahrt]
# 
 1500301; 1509001;     70.84;     70.47;     79.31;     63.12;     14.13;     13.73;      
 9.17;      7.76;      5.60;     10.46;      4.38;      5.04;      
 9.13;      7.73;      5.58;     10.41;      4.37;      5.02; */

class ScenarioA14MagdeburgMStendal {

	static private final String MagdeburgMStendal = "MagdeburgMitte--Stendal";

	static ScenarioForEvalData createNullfall1() {
		// set up the base case:
		ScenarioForEvalData nullfall = new ScenarioForEvalData() ;

		// MagdeburgM-Stendal
		{
			final Values nullfallForOD = new Values() ;
			nullfall.setValuesForODRelation( MagdeburgMStendal, nullfallForOD ) ;
			{
				Mode mode = Mode.Strasse ;
				//aus P2030_2010_A14_induz_ME2.wid
				//XX aus P2030_2010_BMVBS_ME2_131008.csv:
				//priceUser aus P2030_2010_A14_induz_ME2.wid
				{
					DemandSegment segm = DemandSegment.PV_SONST ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.XX), 10862. ) ; // Menge Strasse im Nullfall = verbleibender Verkehr
					nullfallForOD.put(makeKey(mode, segm, Attribute.Reisezeit_h), 86.30/60. ) ; // Fahrzeit Strasse im Nullfall
					nullfallForOD.put(makeKey(mode, segm, Attribute.Distanz_km), 65.72 ) ; // Distanz Strasse im Nullfall

					nullfallForOD.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), 5.04*1.67) ; // Nutzerkosten Strasse im Nullfall --> Belegungsgrad Privatverkehr = 1.74
					nullfallForOD.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 14.13 ) ; // ???  Produktionskosten Strasse im Nullfall
					
				}
			}
			{
				Mode mode = Mode.Bahn ;
				{
					DemandSegment segm = DemandSegment.PV_SONST ;
					nullfallForOD.put(makeKey(mode, segm, Attribute.XX), 8547.) ;   // Menge Bahn im Nullfall.  Wenn nicht bekannt, ggf. die gesamte verlagerte Menge
					nullfallForOD.put(makeKey(mode, segm, Attribute.Reisezeit_h), 77.8/60. ) ; 
					nullfallForOD.put(makeKey(mode, segm, Attribute.Nutzerkosten_Eu), 65.72*0.12) ; // ??? Nutzerkosten Bahn (Strassendistanz mal 12 Cent als Überschlag)
					nullfallForOD.put( makeKey( mode, segm, Attribute.Produktionskosten_Eu ), 0. ) ; // Produktionskosten sind Null bei Bahn.
				}
			}
		}
		// end MagdeburgM-Stendal

		// return the base case:
		return nullfall;
	}

	static ScenarioForEvalData createPlanfallStrassenausbau(ScenarioForEvalData nullfall) {
		// The policy case is initialized as a complete copy of the base case:
		ScenarioForEvalData planfall = nullfall.createDeepCopy() ;

		// MagdeburgM-Stendal
		{
			Values planfallValuesForOD = planfall.getByODRelation(MagdeburgMStendal);
			Assert.assertNotNull(planfallValuesForOD) ;
			{
				DemandSegment segm = DemandSegment.PV_SONST ;
				planfallValuesForOD.inc( makeKey( Mode.Strasse, segm, Attribute.XX), 15784. ) ; // Menge Straße im Planfall.
				planfallValuesForOD.put( makeKey( Mode.Strasse, segm, Attribute.Reisezeit_h), 70.11/60.  ) ; // Fahrzeit Strasse im Planfall 
				planfallValuesForOD.put( makeKey( Mode.Strasse, segm, Attribute.Distanz_km), 65.35 ) ; // Distanz Strasse im Planfall 
				planfallValuesForOD.put (makeKey(Mode.Strasse, segm, Attribute.Nutzerkosten_Eu), 5.02 *1.67); // Nutzerkosten Planfall
				planfallValuesForOD.put (makeKey(Mode.Strasse, segm, Attribute.Produktionskosten_Eu), 13.73); //Produktionskosten Strasse Planfall
				
				double bahnusersNullFalltot =43957.+1606.+4481.+9317.+0.+8547.;
				double demandPvSonstNullfall = 8547. / bahnusersNullFalltot;
				double werktageproJahr = 250.;
				planfallValuesForOD.inc( makeKey( Mode.Bahn, segm, Attribute.XX), -1. * 43.73 * werktageproJahr*demandPvSonstNullfall ) ; // Menge Bahn im Planfall für dieses Demandsegment - es wird angenommen, dass die Verlagerung alle DemandSegmente gleichermassen betrifft. 

				
				// später zu berechnen: Menge Str im Planfall - Menge Str im Nullfall - Menge verlagert = Menge induziert
			}
		}
		// end MagdeburgM-Stendal

		return planfall;
	}

}

