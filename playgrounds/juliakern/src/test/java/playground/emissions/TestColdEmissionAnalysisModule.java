/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestEmission.java                                                       *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.misc.dummyHandler;
import playground.vsp.analysis.modules.emissionsAnalyzer.*;
import playground.vsp.emissions.ColdEmissionAnalysisModule;
import playground.vsp.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import playground.vsp.emissions.events.ColdEmissionEvent;
import playground.vsp.emissions.events.ColdEmissionEventImpl;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.HbefaColdEmissionFactor;
import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler;


public class TestColdEmissionAnalysisModule {

	@Test 
	public void calculateColdEmissionsAndThrowEventTest() {
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
				
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		fillAverageTable(avgHbefaColdTable);
		fillDetailedTable(detailedHbefaColdTable);

		EventsManager emissionEventManager = new dummyHandler();
		ColdEmissionAnalysisModule ceam = new ColdEmissionAnalysisModule(new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable), emissionEventManager, null);
		

		
		//ceam.calculateColdEmissionsAndThrowEvent(coldEmissionEventLinkId, personId, startEngineTime, parkingDuration, accumulatedDistance, vehicleInformation)
		//TODO warum ist hier die Parkzeti als Double in anderen Methoden aber als int?
		//ceam.calculateColdEmissionsAndThrowEvent(coldEmissionEventLinkId, personId, 10.0, 2.0, 	50.0, "PASSENGER_CAR") ;
		//ceam.calculateColdEmissionsAndThrowEvent(new IdImpl("coldEmissionEventLinkId2"),	new IdImpl("personId"), 10.0, 2.0, 	50.0, "PASSENGER_CAR") ;
		
		// 1. Test a cold emission event appearing in the avg. table
		//zeiteinheiten?
		// TODO benennung sinnvoll
		// wird an ; getrennt, sollte vier lang sein, sonst avg. vehicle
		// 1. veh cat, 2. technology, 3. size class, 4. emconcept -- datei die sowas enthaelt ist zbsp efa_coldstart_subsegm_205detailed
		// veh cat "pass. car"
		// technology "PC petrol <1,4L <ECE"
		// size class "petrol (4S)"
		// em concept "<1,4L"
		
		//erste zwei zeilen der coldstart-vehcat-2005average
		/*
		 * Case;VehCat;Year;TrafficScenario;Component;RoadCat;AmbientCondPattern;EFA_weighted;EFA_km_weighted
2005ColdAverage[3.1];pass. car;2005;"BAU" (D);FC;Urban;TØ,0-1h,0-1km;0.99;
		 */
		
		//erluabte strings fuer veh. categorie sind genau und keine anderen!!!
		//PASSENGER_CAR und HEAVY_GOODS_VEHICLE
		//TODO im moment runtime exception wenn etwas anderes uebrgeben wird...
		//-cold emission analysis module convertString2Tuple 
		// XXX falls der erste eintrag einer hbefa veh. category entspricht -> tuple <hbefa veh cat, <'average', 'average', 'average'>> oder tuple < hbefa veh cat, hbefavehattributes>
		// XXX falls der erste eintrag keiner hbefa veh. category entspricht -> tuple <null, <'average', 'average', 'average'>> oder tuple <null, hbefavehattributes>
		// XXX falls der string nicht aus vier teilen besteht -> <hbefa veh cat, <'average', 'average', 'average'>> oder tuple <null, <'average', 'average', 'average'>>
		// TODO soll das so sein? wird mit allen faellen ok umgegangen?
		
		// erster Fall: alle Angaben korrekt: veh cat, technology, size class, em concept
		// passender eintrag im avg table
		
		String vehicleInfoForAvgCase = "PASSENGER_CAR;PC petrol;petrol;none";
		Id idForAvgTable = new IdImpl("link id avg"), personIdForAvgTable = new IdImpl("person avg");
		ceam.calculateColdEmissionsAndThrowEvent(idForAvgTable, personIdForAvgTable, .0, 1., 1., vehicleInfoForAvgCase);
		
		Assert.assertEquals(.07, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
		
		// zweiter Fall: alle Angaben korrekt: veh cat, technology, size class, em concept
		// passender eintrag im detailed table
		dummyHandler.reset();
		String vehicleInfoForDetCase = "PASSENGER_CAR;PC petrol <1,4L <ECE;petrol (4S);<1,4L";
		Id idForDetTable = new IdImpl("linkIddet"), personIdForDetTable = new IdImpl("person det");
		ceam.calculateColdEmissionsAndThrowEvent(idForDetTable, personIdForDetTable, .0, 1., 1., vehicleInfoForDetCase);
		Assert.assertEquals(700., dummyHandler.getSum(), MatsimTestUtils.EPSILON);
		
		// dritter Fall: alle Angaben korrekt: veh cat, technology, size class, em concept
		// passender eintrag im detailed und avg table -> dann soll detailed gewaehlt werden
		dummyHandler.reset();
		String vehicleInfoForBothCase = "PASSENGER_CAR;PC diesel;diesel;>=2L";
		Id idForbothTables = new IdImpl("linkId both"), personIdForBothTables = new IdImpl("person both");
		ceam.calculateColdEmissionsAndThrowEvent(idForbothTables, personIdForBothTables, .0, 1., 1., vehicleInfoForBothCase);
		Assert.assertEquals(70., dummyHandler.getSum(), MatsimTestUtils.EPSILON);
		
		// vierter Fall: 
		// kein passender eintrag im detailed oder avg table
		dummyHandler.reset();
		String vehicleInfoForNoCase = "PASSENGER_CAR;PC diesel;;>=2L";
		Id idForNoTable = new IdImpl("linkId "), personIdForNoTable = new IdImpl("person");
		/**
		 *das wirf nullpointer, da kein passender eintrag im avg. table gefunden wird
		 *TODO soll das so sein? nullpointer garantieren?  
		 *
		ceam.calculateColdEmissionsAndThrowEvent(idForNoTable, personIdForNoTable, .0, 1., 1., vehicleInfoForNoCase);
		Assert.assertEquals(70., dummyHandler.getSum(), MatsimTestUtils.EPSILON);
		*/

		// fuenfter Fall: keine Angaben zu: veh cat, technology, size class, em concept
		dummyHandler.reset();
				String vehInfo5 = ";;;";
				Id linkId5 = new IdImpl("link id 5"), personId5 = new IdImpl("person 5");
				//TODO das macht arrayindex ex. in convertString2Tuple - genauso wie vehInfo5="";
				//ceam.calculateColdEmissionsAndThrowEvent(linkId5, personId5, .0, 1., 1., vehInfo5);
				//Assert.assertEquals(.07, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
		
				
		// sechster Fall: keine Angaben zu: technology, size class, em concept		
				dummyHandler.reset();
				String vehInfo6 = "PASSENGER_CAR;;;";
				Id linkId6 = new IdImpl("link id 6"), personId6 = new IdImpl("person 6");
				ceam.calculateColdEmissionsAndThrowEvent(linkId6, personId6, .0, 1., 1., vehInfo6);
				Assert.assertEquals(.7, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
				
				// siebter Fall: keine Angaben zu: technology, size class, em concept	
				dummyHandler.reset();	
				String vehInfo7 = "PASSENGER_CAR";
				Id linkId7 = new IdImpl("link id 7"), personId7 = new IdImpl("person 7");
				ceam.calculateColdEmissionsAndThrowEvent(linkId7, personId7, .0, 1., 1., vehInfo7);
				Assert.assertEquals(.7, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
				
		// siebter fall heavy goods soll warnmeldung werfen
				// soll table fuer pass cars nehmen -> detailed wenn vorhanden
				dummyHandler.reset();
				String vehInfo8 = "HEAVY_GOODS_VEHICLE;PC petrol;petrol;none";
				Id linkId8= new IdImpl("link id 8"), personId8 = new IdImpl("person 8");
				ceam.calculateColdEmissionsAndThrowEvent(linkId8, personId8, .0, .1, 1., vehInfo8);
				Assert.assertEquals(.07, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
				
		
		//cold emission analysis module mit faktor ungleich 0 - das testet rescale emissions
	}
	
					//TODO varnamen...
	private void fillDetailedTable(
			Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable) {
		
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology("PC petrol <1,4L <ECE");
		vehAtt.setHbefaSizeClass("petrol (4S)");
		vehAtt.setHbefaEmConcept("<1,4L");
		
		HbefaColdEmissionFactor detColdFactor = new HbefaColdEmissionFactor();
		detColdFactor.setColdEmissionFactor(100.); 

		for (ColdPollutant cp: ColdPollutant.values()) {
			HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();	
			detColdKey.setHbefaDistance(1);
			detColdKey.setHbefaParkingTime(1);
			detColdKey.setHbefaVehicleAttributes(vehAtt);
			detColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detColdKey.setHbefaComponent(cp);
			detailedHbefaColdTable.put(detColdKey, detColdFactor);
		}
		
		//"PASSENGER_CAR;PC diesel;diesel;>=2L"		
			vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology("PC diesel");
			vehAtt.setHbefaSizeClass("diesel");
			vehAtt.setHbefaEmConcept(">=2L");

			
			detColdFactor = new HbefaColdEmissionFactor();
			detColdFactor.setColdEmissionFactor(10.); 
			
		for (ColdPollutant cp: ColdPollutant.values()) {			
			HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
			detColdKey.setHbefaDistance(1);
			detColdKey.setHbefaParkingTime(1);	
			detColdKey.setHbefaVehicleAttributes(vehAtt);
			detColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detColdKey.setHbefaComponent(cp);
			detailedHbefaColdTable.put(detColdKey, detColdFactor);
		}
		
		//HEAVY_GOODS_VEHICLE;PC petrol;petrol;none sollte nciht benutzt werden
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology("PC petrol");
		vehAtt.setHbefaSizeClass("petrol");
		vehAtt.setHbefaEmConcept("none");

		
		detColdFactor = new HbefaColdEmissionFactor();
		detColdFactor.setColdEmissionFactor(-1.); 
		
	for (ColdPollutant cp: ColdPollutant.values()) {			
		HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
		detColdKey.setHbefaDistance(1);
		detColdKey.setHbefaParkingTime(1);	
		detColdKey.setHbefaVehicleAttributes(vehAtt);
		detColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
		detColdKey.setHbefaComponent(cp);
		detailedHbefaColdTable.put(detColdKey, detColdFactor);
	}
	}

	private void fillAverageTable(
			Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable) {
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			
			vehAtt.setHbefaEmConcept("average");
			vehAtt.setHbefaSizeClass("average");
			vehAtt.setHbefaTechnology("average");

			
			HbefaColdEmissionFactor avColdFactor = new HbefaColdEmissionFactor();
			avColdFactor.setColdEmissionFactor(.1);	
			
		for (ColdPollutant cp: ColdPollutant.values()) {			
			HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
			avColdKey.setHbefaDistance(1);
			avColdKey.setHbefaParkingTime(1);
			avColdKey.setHbefaVehicleAttributes(vehAtt);
			avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avColdKey.setHbefaComponent(cp);
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
		
				
		/*
		 * 		String vehicleInfoForAvgCase = "PASSENGER_CAR;PC petrol;petrol;none";
		ceam.calculateColdEmissionsAndThrowEvent(idForAvgTable, personIdForAvgTable, .0, 1., 1., vehicleInfoForAvgCase);
		 */
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology("PC petrol");
		vehAtt.setHbefaSizeClass("petrol");
		vehAtt.setHbefaEmConcept("none");

		avColdFactor = new HbefaColdEmissionFactor();
		avColdFactor.setColdEmissionFactor(.01);
		
		for (ColdPollutant cp: ColdPollutant.values()) {	
		HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
		avColdKey.setHbefaDistance(1);
		avColdKey.setHbefaParkingTime(1);
		avColdKey.setHbefaVehicleAttributes(vehAtt);
		avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avColdKey.setHbefaComponent(cp);
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology("PC diesel");
		vehAtt.setHbefaSizeClass("diesel");
		vehAtt.setHbefaEmConcept(">=2L");
		
		avColdFactor = new HbefaColdEmissionFactor();
		avColdFactor.setColdEmissionFactor(.001); 
		
		for (ColdPollutant cp: ColdPollutant.values()) {
			HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
			avColdKey.setHbefaDistance(1);
			avColdKey.setHbefaParkingTime(1);
			avColdKey.setHbefaVehicleAttributes(vehAtt);
			avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avColdKey.setHbefaComponent(cp);			
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
		
		//HEAVY_GOODS_VEHICLE;PC petrol;petrol;none sollte nciht benutzt werden
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology("PC petrol");
		vehAtt.setHbefaSizeClass("petrol");
		vehAtt.setHbefaEmConcept("none");
		
		avColdFactor = new HbefaColdEmissionFactor();
		avColdFactor.setColdEmissionFactor(-1.); 
		
		for (ColdPollutant cp: ColdPollutant.values()) {
			HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
			avColdKey.setHbefaDistance(1);
			avColdKey.setHbefaParkingTime(1);
			avColdKey.setHbefaVehicleAttributes(vehAtt);
			avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
			avColdKey.setHbefaComponent(cp);			
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
	}
	
	@Test @Ignore //is private.... test it anyway?
	//wird implizit oben getestet
	public void calculateColdEmissionsTest() {
		Assert.assertEquals("something", true, true);
	}
	
	@Test @Ignore //is private.... test it anyway?
	//wird implizit oben getestet
	public void convertString2TupleTest(){
		Assert.assertEquals("something", true, true);
	}

	//is private.... test it anyway?
	//TODO sollte rescale Emissions auch getestet werden? ja
}
