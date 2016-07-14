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

package org.matsim.contrib.emissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.HbefaColdEmissionFactor;
import org.matsim.contrib.emissions.types.HbefaColdEmissionFactorKey;
import org.matsim.contrib.emissions.types.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;


/**
 * @author julia
 * 
 * test for playground.vsp.emissions.ColdEmissionAnalysisModule
 * 
 * ColdEmissionAnalysisModule (ceam) 
 * public methods and corresponding tests: 
 * ceam module parameter - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest 
 * ceam - constructor, nothing to test 
 * reset - nothing to test
 * calculate cold emissions and throw event - calculateColdEmissionsAndThrowEventTest
 * 
 * private methods and corresponding tests:
 * rescale cold emissions - rescaleColdEmissionsTest
 * calculate cold emissions - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest 
 * convert string to tuple - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * 
 * in all cases the needed tables are created manually by the setUp() method
 * see test methods for details on the particular test cases
 */

public class TestColdEmissionAnalysisModule {

    private ColdEmissionAnalysisModule ceam;
	
	private final String passengercar= "PASSENGER_CAR";
    private final Double startTime = 0.0;
    private final Double parkingDuration =1.;
	// same values as int for table
    private final int tableParkingDuration= (int) Math.round(parkingDuration);
    private final int tableAccDistance = 1;
	private final int numberOfColdEmissions = ColdPollutant.values().length;
	
	// strings for test cases
	// first case: complete data - corresponding entry in average table
    private final String pcpetrol ="PC petrol";
    private final String petrol= "petrol";
    private final String none="none";
	// second case: complete data - corresponding entry in detailed table
    private final String pcpetrol14 = "PC petrol <1,4L <ECE";
    private final String petrol4S = "petrol (4S)";
    private final String leq14l = "<1,4L";
	// third case: complete data corresponding entries in average and detailed table - use detailed
    private final String pcdiesel = "PC diesel";
    private final String diesel = "diesel";
    private final String geq2l = ">=2L";
	// fifth case: cold emission factor not set
    private final String nullcase = "nullCase";

    // emission factors for tables - no dublicates!
    private final Double detailedAverageFactor = 100.;
    private final Double dieselFactor = 10.;
    private final Double heavygoodsFactor= -1.;
	private final Double averageAverageFactor = .1;
    private final Double petrolFactor = .01;

    private boolean excep =false;
	
	@Test 
	public void calculateColdEmissionsAndThrowEventTest_completeData() {
		
		/*
		 * six test cases with complete input data 
		 * or input that should be assigned to average/default cases
		 */
		
		setUp();
		
		List<ArrayList> testCases = new ArrayList<>();

		ArrayList<Object> testCase1= new ArrayList<>(), testCase2= new ArrayList<>();
		ArrayList<Object> testCase3= new ArrayList<>(), testCase4= new ArrayList<>();
		ArrayList<Object> testCase5= new ArrayList<>(), testCase6= new ArrayList<>();
		
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
        String heavygoodsvehicle = "HEAVY_GOODS_VEHICLE";
        Collections.addAll(testCase6, heavygoodsvehicle, pcpetrol, petrol, none, petrolFactor);
		
		testCases.add(testCase1);testCases.add(testCase2);testCases.add(testCase3);
		testCases.add(testCase4);testCases.add(testCase5);testCases.add(testCase6);
		
		for(List<Object> tc : testCases){
			HandlerToTestEmissionAnalysisModules.reset();
			Id<Link> linkId = Id.create("linkId"+testCases.indexOf(tc), Link.class);
			Id<Vehicle> vehicleId = Id.create("vehicleId"+testCases.indexOf(tc), Vehicle.class);
			Id<VehicleType> vehicleTypeId = Id.create(tc.get(0) +";"+ tc.get(1) +";"+ tc.get(2) +";"+ tc.get(3), VehicleType.class);
			ceam.calculateColdEmissionsAndThrowEvent(linkId, vehicleId, startTime, parkingDuration, tableAccDistance, vehicleTypeId);
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
		List<Id<VehicleType>> testCasesExceptions = new ArrayList<>();
		excep  = false;
		
		// seventh case: no corresponding entry either in the detailed nor the average table
        Id<VehicleType> vehicleInfoForNoCase = Id.create("PASSENGER_CAR;PC diesel;;>=2L", VehicleType.class);
        testCasesExceptions.add(vehicleInfoForNoCase);
		// eighth case: vehicle category not specified
		testCasesExceptions.add(Id.create(";;;", VehicleType.class));
		// ninth case: empty string as id
		testCasesExceptions.add(Id.create("", VehicleType.class));
		// tenth case: null id
		testCasesExceptions.add(null);
		
		for(Id<VehicleType> vehicleTypeId : testCasesExceptions){
			String message ="'"+vehicleTypeId+"'"+ " was used to calculate cold emissions and throw an event." 
					+ "It should throw an exception because it is not valid vehicle information string.";
			try{
				Id<Link> linkId = Id.create("linkId"+testCasesExceptions.indexOf(vehicleTypeId), Link.class);
				Id<Vehicle> vehicleId = Id.create("vehicleId"+testCasesExceptions.indexOf(vehicleTypeId), Vehicle.class);
				ceam.calculateColdEmissionsAndThrowEvent(linkId, vehicleId, startTime, parkingDuration, tableAccDistance, vehicleTypeId);
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
		Id<VehicleType> vehInfo11 = Id.create(passengercar, VehicleType.class); 
		Id<Link> linkId11 = Id.create("link id 11", Link.class);
		Id<Vehicle> vehicleId7 = Id.create("vehicle 11", Vehicle.class);
		
		HandlerToTestEmissionAnalysisModules.reset();
		ceam.calculateColdEmissionsAndThrowEvent(linkId11, vehicleId7, startTime, parkingDuration, tableAccDistance, vehInfo11);
		String message = "The expected emissions for an event with vehicle information string '" + vehInfo11+ "' are " + 
				numberOfColdEmissions*averageAverageFactor + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		Assert.assertEquals(message, numberOfColdEmissions*averageAverageFactor, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);

	}
	
	@Test 
	public void rescaleColdEmissionsTest() {
		
		// can not use the setUp method here because the efficiency factor is not null
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<>();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<>();
		fillAverageTable(avgHbefaColdTable);
		fillDetailedTable(detailedHbefaColdTable);

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
        Double rescaleFactor = -.001;
        ColdEmissionAnalysisModule ceam = new ColdEmissionAnalysisModule(new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable), emissionEventManager, rescaleFactor);
		HandlerToTestEmissionAnalysisModules.reset();
		
		Id<Link> idForAvgTable = Id.create("link id avg", Link.class);
		Id<Vehicle> vehicleIdForAvgTable = Id.create("vehicle avg", Vehicle.class);
        Id<VehicleType> vehicleInfoForAvgCase = Id.create("PASSENGER_CAR;PC petrol;petrol;none", VehicleType.class);
        ceam.calculateColdEmissionsAndThrowEvent(idForAvgTable, vehicleIdForAvgTable, startTime, parkingDuration, tableAccDistance, vehicleInfoForAvgCase);
		String message = "The expected rescaled emissions for this event are (calculated emissions * rescalefactor) = " 
				+ (numberOfColdEmissions*petrolFactor) + " * " + rescaleFactor + " = " +
				(numberOfColdEmissions*petrolFactor* rescaleFactor) + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		Assert.assertEquals(message, rescaleFactor *numberOfColdEmissions*petrolFactor, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		
	}
	
	private void setUp() {
        Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<>();
        Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<>();
		
		fillAverageTable(avgHbefaColdTable);
		fillDetailedTable(detailedHbefaColdTable);

        EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		ceam = new ColdEmissionAnalysisModule(new ColdEmissionAnalysisModuleParameter(avgHbefaColdTable, detailedHbefaColdTable), emissionEventManager, null);
		
	}
	
	private void fillDetailedTable(Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable) {
		// create all needed and one unneeded entry for the detailed table
		
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
			// create all needed and one unneeded entry for the average table
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
        Double averageDieselFactor = .001;
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
