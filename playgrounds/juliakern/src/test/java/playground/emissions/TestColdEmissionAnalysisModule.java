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
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.misc.HandlerToTestEmissionAnalysisModules;
import playground.vsp.emissions.ColdEmissionAnalysisModule;
import playground.vsp.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.HbefaColdEmissionFactor;
import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;

/*
 * test for playground.vsp.emissions.ColdEmissionAnalysisModule
 * 
 * ColdEmissionAnalysisModule (ceam) 
 * public: 
 * ceam module parameter - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest 
 * ceam - constructor, nothing to test 
 * reset - nothing to test
 * calculate cold emissions and throw event - calculateColdEmissionsAndThrowEventTest
 * 
 * private:
 * rescale cold emissions - rescaleColdEmissionsTest
 * calculate cold emissions - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest 
 * convert string to tuple - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 */

public class TestColdEmissionAnalysisModule {

	Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
	Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;
	EventsManager emissionEventManager;
	ColdEmissionAnalysisModule ceam;
	
	String passengercar= "PASSENGER_CAR", heavygoodsvehicle="HEAVY_GOODS_VEHICLE"; 
	Double startTime = 0.0, parkingDuration =1., accDistance = 1.;
	// same values as int for table
	int tableParkingDuration= (int) Math.round(parkingDuration), tableAccDistance = (int) Math.round(accDistance);
	int numberOfColdEmissions = ColdPollutant.values().length;
	
	// strings for test cases
	// first case: complete data - corresponding entry in average table
	String pcpetrol ="PC petrol", petrol= "petrol", none="none";
	// second case: complete data - corresponding entry in detailed table
	String pcpetrol14 = "PC petrol <1,4L <ECE", petrol4S = "petrol (4S)",leq14l = "<1,4L";
	// third case: complete data corresponding entries in average and detailed table - use detailed
	String pcdiesel = "PC diesel", diesel = "diesel", geq2l = ">=2L";
	// fifth case: cold emission factor not set
	String nullcase = "nullCase", vehicleInfoForNoCase = "PASSENGER_CAR;PC diesel;;>=2L";
	String vehicleInfoForAvgCase = "PASSENGER_CAR;PC petrol;petrol;none";
	
	// emission factors for tables - no dublicates!
	Double detailedAverageFactor = 100., dieselFactor = 10., heavygoodsFactor= -1.; 
	Double averageAverageFactor = .1, petrolFactor = .01, averageDieselFactor = .001; 
	
	Double rescaleFactor = -.001;
	boolean excep =false;
	
	@Test 
	public void calculateColdEmissionsAndThrowEventTest_completeData() {
		
		/*
		 * six test cases with complete input data 
		 * or input that should be assigned to average/default cases
		 */
		
		setUp();
		
		List<ArrayList> testCases = new ArrayList<ArrayList>();		

		ArrayList<Object> testCase1= new ArrayList<Object>(), testCase2= new ArrayList<Object>();
		ArrayList<Object> testCase3= new ArrayList<Object>(), testCase4= new ArrayList<Object>();
		ArrayList<Object> testCase5= new ArrayList<Object>(), testCase6= new ArrayList<Object>();
		
		// first case: complete data
		// corresponding entry in average table
		Collections.addAll(testCase1, passengercar, pcpetrol, petrol, none, petrolFactor);
		// second case: complete data
		// corresponding entry in detailed table
		Collections.addAll(testCase2, passengercar, pcpetrol14, petrol4S, leq14l, detailedAverageFactor);
		// third case: complete data
		// corresponding entries in average and detailed table
		// -> use detailed
		Collections.addAll(testCase3, passengercar, pcdiesel, diesel, geq2l, dieselFactor);
		// fourth case: no specifications for technology, size class or em concept
		// -> use average table	
		Collections.addAll(testCase4, passengercar, "", "", "", averageAverageFactor);		
		// fifth case: cold emission factor not set - handled as 0.0
		// TODO beim erstellen ueberpruefen dann test umschreiben
		Collections.addAll(testCase5, passengercar, pcpetrol, petrol, nullcase, .0);		
		// sixth case: heavy goods vehicle 
		// -> throw warning -> use detailed or average table for passenger cars
		Collections.addAll(testCase6, heavygoodsvehicle, pcpetrol, petrol, none, petrolFactor);
		
		testCases.add(testCase1);testCases.add(testCase2);testCases.add(testCase3);
		testCases.add(testCase4);testCases.add(testCase5);testCases.add(testCase6);
		
		for(List<Object> tc : testCases){
			HandlerToTestEmissionAnalysisModules.reset();
			Id linkId = new IdImpl("linkId"+testCases.indexOf(tc));
			Id personId = new IdImpl("personId"+testCases.indexOf(tc));
			String vehicleInfo = (String) tc.get(0)+";"+(String) tc.get(1)+";"+(String) tc.get(2)+";"+(String) tc.get(3);
			ceam.calculateColdEmissionsAndThrowEvent(linkId, personId, startTime, parkingDuration, accDistance, vehicleInfo);
			String message = "The expected emissions for " + tc.toString() + " are " + 
						numberOfColdEmissions*(Double)tc.get(4) + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
			Assert.assertEquals(message, numberOfColdEmissions*(Double)tc.get(4), HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		}
		
	}

	@Test 
	public void calculateColdEmissionsAndThrowEventTest_Exceptions() {
		
		/*
		 * four test cases 
		 * all of them should throw exceptions
		 */
		
		setUp();
		List<String> testCasesExceptions = new ArrayList<String>();		
		excep  = false;
		
		// seventh case: no corresponding entry either in the detailed nor the average table
		testCasesExceptions.add(vehicleInfoForNoCase);
		// eighth case: vehicle category not specified
		testCasesExceptions.add(";;;");
		// ninth case: empty string
		testCasesExceptions.add("");
		// tenth case: null string
		testCasesExceptions.add(null);
		
		for(String vehinfo : testCasesExceptions){
			String message ="'"+vehinfo+"'"+ " was used to calculate cold emissions and throw an event." 
					+ "It should throw an exception because it is not valid vehicle information string.";
			try{
				Id linkId = new IdImpl("linkId"+testCasesExceptions.indexOf(vehinfo));
				Id personId = new IdImpl("personId"+testCasesExceptions.indexOf(vehinfo));
				ceam.calculateColdEmissionsAndThrowEvent(linkId, personId, startTime, parkingDuration, accDistance, vehinfo);
			}catch(Exception e){
				excep=true;
			}
			Assert.assertTrue(message, excep);
			excep=false;
		}
		
	}
	
	@Test 
	public void calculateColdEmissionsAndThrowEventTest_minimalVehicleInformation() {
		
		setUp();
		excep  = false;
				
		// eleventh case: no specifications for technology, size, class, em concept
		// string has no semicolons as seperators - use average values
		String vehInfo11 = passengercar; 
		Id linkId11 = new IdImpl("link id 11"), personId7 = new IdImpl("person 11");
		
		HandlerToTestEmissionAnalysisModules.reset();
		ceam.calculateColdEmissionsAndThrowEvent(linkId11, personId7, startTime, parkingDuration, accDistance, vehInfo11);
		String message = "The expected emissions for an event with vehicle information string '" + vehInfo11+ "' are " + 
				numberOfColdEmissions*averageAverageFactor + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		Assert.assertEquals(message, numberOfColdEmissions*averageAverageFactor, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);

	}
	
	@Test 
	public void rescaleColdEmissionsTest() {
		
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		fillAverageTable(avgHbefaColdTable);
		fillDetailedTable(detailedHbefaColdTable);

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		ColdEmissionAnalysisModule ceam = new ColdEmissionAnalysisModule(new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable), emissionEventManager, rescaleFactor);
		HandlerToTestEmissionAnalysisModules.reset();
		
		Id idForAvgTable = new IdImpl("link id avg"), personIdForAvgTable = new IdImpl("person avg");
		ceam.calculateColdEmissionsAndThrowEvent(idForAvgTable, personIdForAvgTable, startTime, parkingDuration, accDistance, vehicleInfoForAvgCase);
		String message = "The expected rescaled emissions for this event are (calculated emissions * rescalefactor) = " 
				+ (numberOfColdEmissions*petrolFactor) + " * " + rescaleFactor + " = " +
				(numberOfColdEmissions*petrolFactor*rescaleFactor) + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		Assert.assertEquals(message, rescaleFactor*numberOfColdEmissions*petrolFactor, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		
	}
	
	private void setUp() {
		avgHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();	
		detailedHbefaColdTable = new HashMap<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor>();
		
		fillAverageTable(avgHbefaColdTable);
		fillDetailedTable(detailedHbefaColdTable);

		emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		ceam = new ColdEmissionAnalysisModule(new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable), emissionEventManager, null);
		
	}
	
	private void fillDetailedTable(Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable) {
		
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcpetrol14);
		vehAtt.setHbefaSizeClass(petrol4S);
		vehAtt.setHbefaEmConcept(leq14l);
		
		HbefaColdEmissionFactor detColdFactor = new HbefaColdEmissionFactor();
		detColdFactor.setColdEmissionFactor(detailedAverageFactor); 

		for (ColdPollutant cp: ColdPollutant.values()) {
			HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();	
			detColdKey.setHbefaDistance(tableAccDistance);
			detColdKey.setHbefaParkingTime(tableParkingDuration);
			detColdKey.setHbefaVehicleAttributes(vehAtt);
			detColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detColdKey.setHbefaComponent(cp);
			detailedHbefaColdTable.put(detColdKey, detColdFactor);
		}
			
			vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology(pcdiesel);
			vehAtt.setHbefaSizeClass(diesel);
			vehAtt.setHbefaEmConcept(geq2l);

			detColdFactor = new HbefaColdEmissionFactor();
			detColdFactor.setColdEmissionFactor(dieselFactor); 
			
		for (ColdPollutant cp: ColdPollutant.values()) {			
			HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
			detColdKey.setHbefaDistance(tableAccDistance);
			detColdKey.setHbefaParkingTime(tableParkingDuration);	
			detColdKey.setHbefaVehicleAttributes(vehAtt);
			detColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detColdKey.setHbefaComponent(cp);
			detailedHbefaColdTable.put(detColdKey, detColdFactor);
		}
		
		//HEAVY_GOODS_VEHICLE;PC petrol;petrol;none should not be used
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcpetrol);
		vehAtt.setHbefaSizeClass(petrol);
		vehAtt.setHbefaEmConcept(none);

		
		detColdFactor = new HbefaColdEmissionFactor();
		detColdFactor.setColdEmissionFactor(heavygoodsFactor); 
		
	for (ColdPollutant cp: ColdPollutant.values()) {			
		HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
		detColdKey.setHbefaDistance(tableAccDistance);
		detColdKey.setHbefaParkingTime(tableParkingDuration);	
		detColdKey.setHbefaVehicleAttributes(vehAtt);
		detColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
		detColdKey.setHbefaComponent(cp);
		detailedHbefaColdTable.put(detColdKey, detColdFactor);
	}
	
	//"PASSENGER_CAR;PC petrol;petrol;nullCase"
	vehAtt = new HbefaVehicleAttributes();
	vehAtt.setHbefaTechnology(pcpetrol);
	vehAtt.setHbefaSizeClass(petrol);
	vehAtt.setHbefaEmConcept(nullcase);

	
	detColdFactor = new HbefaColdEmissionFactor();
	//detColdFactor.setColdEmissionFactor(null); 
	
for (ColdPollutant cp: ColdPollutant.values()) {			
	HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
	detColdKey.setHbefaDistance(tableAccDistance);
	detColdKey.setHbefaParkingTime(tableParkingDuration);	
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
			avColdFactor.setColdEmissionFactor(averageAverageFactor);	
			
		for (ColdPollutant cp: ColdPollutant.values()) {			
			HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
			avColdKey.setHbefaDistance(tableAccDistance);
			avColdKey.setHbefaParkingTime(tableParkingDuration);
			avColdKey.setHbefaVehicleAttributes(vehAtt);
			avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avColdKey.setHbefaComponent(cp);
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcpetrol);
		vehAtt.setHbefaSizeClass(petrol);
		vehAtt.setHbefaEmConcept(none);

		avColdFactor = new HbefaColdEmissionFactor();
		avColdFactor.setColdEmissionFactor(petrolFactor);
		
		for (ColdPollutant cp: ColdPollutant.values()) {	
			HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
			avColdKey.setHbefaDistance(tableAccDistance);
			avColdKey.setHbefaParkingTime(tableParkingDuration);
			avColdKey.setHbefaVehicleAttributes(vehAtt);
			avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avColdKey.setHbefaComponent(cp);
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
		
		
		// duplicate from detailed table (different emission factor though)
		// this should not be used but is needed to assure that the detailed table is tried before the average table
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcdiesel);
		vehAtt.setHbefaSizeClass(diesel);
		vehAtt.setHbefaEmConcept(geq2l);
		
		avColdFactor = new HbefaColdEmissionFactor();
		avColdFactor.setColdEmissionFactor(averageDieselFactor); 
		
		for (ColdPollutant cp: ColdPollutant.values()) {
			HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
			avColdKey.setHbefaDistance(tableAccDistance);
			avColdKey.setHbefaParkingTime(tableParkingDuration);
			avColdKey.setHbefaVehicleAttributes(vehAtt);
			avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avColdKey.setHbefaComponent(cp);			
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
		
		//HEAVY_GOODS_VEHICLE;PC petrol;petrol;none should not be used
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcpetrol);
		vehAtt.setHbefaSizeClass(petrol);
		vehAtt.setHbefaEmConcept(none);
		
		avColdFactor = new HbefaColdEmissionFactor();
		avColdFactor.setColdEmissionFactor(heavygoodsFactor); 
		
		for (ColdPollutant cp: ColdPollutant.values()) {
			HbefaColdEmissionFactorKey avColdKey = new HbefaColdEmissionFactorKey();
			avColdKey.setHbefaDistance(tableAccDistance);
			avColdKey.setHbefaParkingTime(tableParkingDuration);
			avColdKey.setHbefaVehicleAttributes(vehAtt);
			avColdKey.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
			avColdKey.setHbefaComponent(cp);			
			avgHbefaColdTable.put(avColdKey, avColdFactor);
		}
	}
	
	
	
}
