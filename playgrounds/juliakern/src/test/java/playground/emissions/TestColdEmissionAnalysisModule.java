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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.misc.dummyHandler;
import playground.vsp.emissions.ColdEmissionAnalysisModule;
import playground.vsp.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.HbefaColdEmissionFactor;
import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;


public class TestColdEmissionAnalysisModule {

	@Test 
	public void calculateColdEmissionsAndThrowEventTest() {
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
				
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		fillAverageTable(avgHbefaColdTable);
		fillDetailedTable(detailedHbefaColdTable);

		EventsManager emissionEventManager = new dummyHandler();
		ColdEmissionAnalysisModule ceam = new ColdEmissionAnalysisModule(new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable), emissionEventManager, null);
		
		
		//erluabte strings fuer veh. categorie sind genau folgende und keine anderen!!!
		//PASSENGER_CAR und HEAVY_GOODS_VEHICLE
		
		int numberOfColdEmissions = 7;
		Double startTime = 0.0, parkingDuration =1., accDistance = 1.;
		//String startTime = "0.0", parkingDuration ="1.", accDistance = "1.";
		List<ArrayList> testCases = new ArrayList<ArrayList>();		

		ArrayList<Object> testCase1= new ArrayList<Object>(), testCase2= new ArrayList<Object>();
		ArrayList<Object> testCase3= new ArrayList<Object>(), testCase4= new ArrayList<Object>();
		ArrayList<Object> testCase5= new ArrayList<Object>(), testCase6= new ArrayList<Object>();
		
		// erster Fall: alle Angaben korrekt: veh cat, technology, size class, em concept
		// passender eintrag im avg table
		Collections.addAll(testCase1, "PASSENGER_CAR", "PC petrol", "petrol", "none", startTime, parkingDuration, accDistance, .01);
		// zweiter Fall: alle Angaben korrekt: veh cat, technology, size class, em concept
		// passender eintrag im detailed table
		Collections.addAll(testCase2, "PASSENGER_CAR", "PC petrol <1,4L <ECE", "petrol (4S)", "<1,4L", startTime, parkingDuration, accDistance, 100.);
		// dritter Fall: alle Angaben korrekt: veh cat, technology, size class, em concept
		// passender eintrag im detailed und avg table -> dann soll detailed gewaehlt werden
		Collections.addAll(testCase3, "PASSENGER_CAR", "PC diesel", "diesel", ">=2L", startTime, parkingDuration, accDistance, 10.);
		// sechster Fall: keine Angaben zu: technology, size class, em concept	
		Collections.addAll(testCase4, "PASSENGER_CAR", "", "", "", startTime, parkingDuration, accDistance, .1);		
		// neunter fall cold emission factor nicht gesetzt - TODO benjamin-> kann das passieren?
				// wird wie 0.0 behandelt
		Collections.addAll(testCase5, "PASSENGER_CAR", "PC petrol", "petrol", "nullCase", startTime, parkingDuration, accDistance, .0);		
		// achter fall heavy goods soll warnmeldung werfen
				// soll table fuer pass cars nehmen -> detailed wenn vorhanden
		Collections.addAll(testCase6, "HEAVY_GOODS_VEHICLE", "PC petrol", "petrol", "none", startTime, parkingDuration, accDistance, .01);
		
		testCases.add(testCase1);testCases.add(testCase2);testCases.add(testCase3);
		testCases.add(testCase4);testCases.add(testCase5);testCases.add(testCase6);

		
		for(List<Object> tc : testCases){
			dummyHandler.reset();
			Id linkId = new IdImpl("linkId"+testCases.indexOf(tc));
			Id personId = new IdImpl("personId"+testCases.indexOf(tc));
			String vehicleInfo = (String) tc.get(0)+";"+(String) tc.get(1)+";"+(String) tc.get(2)+";"+(String) tc.get(3);
			ceam.calculateColdEmissionsAndThrowEvent(linkId, personId, (Double)tc.get(4), (Double)tc.get(5), (Double)tc.get(6), vehicleInfo);
			Assert.assertEquals(numberOfColdEmissions*(Double)tc.get(7), dummyHandler.getSum(), MatsimTestUtils.EPSILON);

		}
				
				
				// vierter Fall: 
				// kein passender eintrag im detailed oder avg table
				dummyHandler.reset();
				String vehicleInfoForNoCase = "PASSENGER_CAR;PC diesel;;>=2L";
				Id idForNoTable = new IdImpl("linkId "), personIdForNoTable = new IdImpl("person");
				boolean excep =false;
				try{
							ceam.calculateColdEmissionsAndThrowEvent(idForNoTable, personIdForNoTable, .0, 1., 1., vehicleInfoForNoCase);
				Assert.assertEquals(70., dummyHandler.getSum(), MatsimTestUtils.EPSILON);
				}catch(NullPointerException e){
					//TODO soll das so sein? nullpointer garantieren?  
					excep  = true;
				}
				Assert.assertTrue(excep);
				excep=false;
				

				// fuenfter Fall: keine Angaben zu: veh cat, technology, size class, em concept
				dummyHandler.reset();
						String vehInfo5 = ";;;";
						Id linkId5 = new IdImpl("link id 5"), personId5 = new IdImpl("person 5");
						try{
							ceam.calculateColdEmissionsAndThrowEvent(linkId5, personId5, .0, 1., 1., vehInfo5);
							Assert.assertEquals(.07, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
						}catch(ArrayIndexOutOfBoundsException e){
							excep = true;
						}
						Assert.assertTrue(excep);
						//TODO das macht arrayindex ex. in convertString2Tuple - genauso wie vehInfo5="";	
						
						// siebter Fall: keine Angaben zu: technology, size class, em concept	
				dummyHandler.reset();	
				String vehInfo7 = "PASSENGER_CAR";
				Id linkId7 = new IdImpl("link id 7"), personId7 = new IdImpl("person 7");
				ceam.calculateColdEmissionsAndThrowEvent(linkId7, personId7, .0, 1., 1., vehInfo7);
				Assert.assertEquals(.7, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
	}
	
	@Test 
	public void rescaleColdEmissionsTest() {
				//cold emission analysis module mit faktor ungleich 0 - das testet rescale emissions
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
				
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		fillAverageTable(avgHbefaColdTable);
		//fillDetailedTable(detailedHbefaColdTable);

		EventsManager emissionEventManager = new dummyHandler();
		Double rescaleFactor = -.001;
		ColdEmissionAnalysisModule ceam = 
				new ColdEmissionAnalysisModule(new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable), emissionEventManager, rescaleFactor );
		dummyHandler.reset();
		String vehicleInfoForAvgCase = "PASSENGER_CAR;PC petrol;petrol;none";
		Id idForAvgTable = new IdImpl("link id avg"), personIdForAvgTable = new IdImpl("person avg");
		ceam.calculateColdEmissionsAndThrowEvent(idForAvgTable, personIdForAvgTable, .0, 1., 1., vehicleInfoForAvgCase);
		Assert.assertEquals(.07*rescaleFactor, dummyHandler.getSum(), MatsimTestUtils.EPSILON);
		//TODO cold emission factor koennte 'null' sein
		
	}
	
					//TODO varnamen... von oben auslesen
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
	//"PASSENGER_CAR;PC petrol;petrol;nullCase"
	vehAtt = new HbefaVehicleAttributes();
	vehAtt.setHbefaTechnology("PC petrol");
	vehAtt.setHbefaSizeClass("petrol");
	vehAtt.setHbefaEmConcept("nullCase");

	
	detColdFactor = new HbefaColdEmissionFactor();
	//detColdFactor.setColdEmissionFactor(null); 
	
for (ColdPollutant cp: ColdPollutant.values()) {			
	HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
	detColdKey.setHbefaDistance(1);
	detColdKey.setHbefaParkingTime(1);	
	detColdKey.setHbefaVehicleAttributes(vehAtt);
	detColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
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
