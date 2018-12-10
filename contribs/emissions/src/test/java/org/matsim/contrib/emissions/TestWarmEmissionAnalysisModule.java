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

import java.util.*;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.roadTypeMapping.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.types.*;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;


/**
 * @author julia
 

/*
 * test for playground.vsp.emissions.WarmEmissionAnalysisModule
 * 
 * WarmEmissionAnalysisModule (weam) 
 * public methods and corresponding tests: 
 * weamParameter - testWarmEmissionAnalysisParameter
 * throw warm EmissionEvent - testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * check vehicle info and calculate warm emissions -testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * get free flow occurences - testCounters*()
 * get fraction occurences - testCounters*()
 * get stop go occurences - testCounters*()
 * get km counter - testCounters*()
 * get free flow km counter - testCounters*()
 * get top go km couter - testCounters*()
 * get warm emission event counter - testCounters*()
 * 
 * private methods and corresponding tests: 
 * rescale warm emissions - rescaleWarmEmissionsTest()
 * calculate warm emissions - implicitly tested
 * convert string 2 tuple - implicitly tested
 * 
 * in all cases the needed tables are created manually by the setUp() method
 * see test methods for details on the particular test cases
 **/
 

public class TestWarmEmissionAnalysisModule {

	//Old list of pollutants
	private final Set<String> pollutants = new HashSet<>(Arrays.asList("CO", "CO2(total)", "FC", "HC", "NMHC", "NOx", "NO2","PM", "SO2"));
	private final int numberOfWarmPollutants = pollutants.size();
	private final String hbefaRoadCategory = "URB";
    private final String roadType = "0";
	private final int leaveTime = 0;
	private boolean excep =false;
	private final String passengercar= "PASSENGER_CAR";

    private HbefaRoadTypeMapping roadTypeMapping;
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	private Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds;

    private WarmEmissionAnalysisModule weam;
	private Map<String, Double> warmEmissions;
	
	// saturated and heavy not used so far -> not tested
    private final HbefaTrafficSituation trafficSituationff = HbefaTrafficSituation.FREEFLOW;
	private final HbefaTrafficSituation trafficSituationsg = HbefaTrafficSituation.STOPANDGO;

	//average speeds should be the same across all car types, but vary by traffic situation
	private final Double avgPassengerCarSpeedff = 20.;
	private final Double avgPassengerCarSpeedSg = 10.;
	
	// emission factors for tables - no dublicates!
    private final Double detailedPetrolFactorFf = .1;
    private final Double detailedZeroFactorFf  =  .0011;
    private final Double detailedSgffFactorFf =   .000011;
	private final Double detailedSgffFactorSg = 	.0000011;
	private final Double avgPcFactorFf = 1.;
	private final Double avgPcFactorSg = 10.;

    // vehicle information for regular test cases
	// case 1 - data in both tables -> use detailed
    private final String 	petrolTechnology = "PC petrol <1,4L";
    private final String petrolSizeClass ="<ECE petrol (4S)";
    private final String petrolConcept ="<1,4L";
	private final Double petrolSpeedFf = avgPassengerCarSpeedff;
    private final Double petrolSpeedSg = avgPassengerCarSpeedSg;
	// case 2 - free flow entry in both tables, stop go entry in average table -> use average
    private final String 	pcTechnology = "PC petrol <1,4L <ECE";
    private final String pcSizeClass = "petrol (4S)";
    private final String pcConcept = "<1,4L";
	private final Double pcfreeVelocity = avgPassengerCarSpeedff;
    private final Double pcsgVelocity= avgPassengerCarSpeedSg;
	private double detailedPcFactorFf = .0001;

	// case 3 - stop go entry in both tables, free flow entry in average table -> use average
    private final String 	dieselTechnology = "PC diesel";
    private final String dieselSizeClass = "diesel";
    private final String dieselConcept = ">=2L";
	private final Double dieselFreeVelocity = avgPassengerCarSpeedff;
    private final Double dieselSgVelocity = avgPassengerCarSpeedSg;
	// case 4 - data in average table
    private final String 	lpgTechnology = "PC LPG Euro-4";
    private final String lpgSizeClass = "LPG";
    private final String lpgConcept = "not specified";
	private final Double lpgFreeVelocity = avgPassengerCarSpeedff;
    private final Double lpgSgVelocity = avgPassengerCarSpeedSg;
    private final Double noeFreeSpeed = avgPassengerCarSpeedff;
	// case 6 - data in detailed table, stop go speed zero
	private final String zeroRoadCatgory = "URB_case6";
	private final String zeroTechnology = "zero technology";
    private final String zeroConcept = "zero concept";
    private final String zeroSizeClass = "zero size class";
	private final Double zeroFreeVelocity = avgPassengerCarSpeedff;
    private final Double zeroSgVelocity = 0.;

	// case 7 - data in detailed table, stop go speed = free flow speed
	private final String sgffRoadCatgory = "URB_case7";
	private final String sgffTechnology = "sg ff technology";
    private final String sgffConcept = "sg ff concept";
    private final String sgffSizeClass = "sg ff size class";
	private final Double sgffDetailedFfSpeed = avgPassengerCarSpeedff;
	private final Double sgffDetailedsgSpeed = avgPassengerCarSpeedff;
    //This case is no longer valid
	// case 10 - data in detailed table, stop go speed > free flow speed
    private final String tableTechnology = "petrol (4S)";
    private final String tableSizeClass= "<1,4L";
    private final String tableConcept = "PC-P-Euro-0";
	private final Double tableffSpeed = avgPassengerCarSpeedff;
	private final double tablesgSpeed = avgPassengerCarSpeedSg;

	@Test
	public void testWarmEmissionAnalysisParameter(){
		setUp();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setUsingVehicleTypeIdAsVehicleDescription(true);

		WarmEmissionAnalysisModuleParameter weamp
				= new WarmEmissionAnalysisModuleParameter(avgHbefaWarmTable, null, hbefaRoadTrafficSpeeds, pollutants, ecg);
		Assert.assertEquals(weamp.getClass(), WarmEmissionAnalysisModuleParameter.class);
		weamp = new WarmEmissionAnalysisModuleParameter(null, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, ecg);
		Assert.assertEquals(weamp.getClass(), WarmEmissionAnalysisModuleParameter.class);
	}
	
	//@Test
	//public void testWarmEmissionAnalysisModule_exceptions(){
		
		/* out-dated 
		 * the constructor aborts if either the 
		 * warm emission analysis module parameter or
		 * the events mangager is null
		 * EmissionEfficiencyFactor = 'null' is allowed and therefore not tested here
		 */
		
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent1(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 1 - data in both tables -> use detailed
		Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);
		double linkLength = 200.;

		Link mockLink = createMockLink("link 1", linkLength, petrolSpeedFf / 3.6);

		Id<VehicleType> vehicleTypeId = Id.create(passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength/petrolSpeedFf*3.6);
		Assert.assertEquals(detailedPetrolFactorFf*linkLength/1000., warmEmissions.get("CO2(total)"), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		
		weam.throwWarmEmissionEvent(leaveTime, mockLink.getId(), vehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*detailedPetrolFactorFf*linkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}

	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent2(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		Id<Vehicle> pcVehicleId = Id.create("veh 2", Vehicle.class);
		double pclinkLength= 100.;
		Link pclink = createMockLink("link 2", pclinkLength, pcfreeVelocity / 3.6);

		Id<VehicleType> pcVehicleTypeId = Id.create(passengercar + ";"+ pcTechnology + ";"+pcSizeClass+";"+pcConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle pcVehicle = vehFac.createVehicle(pcVehicleId, vehFac.createVehicleType(pcVehicleTypeId));
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle, pclink, pclinkLength/pcfreeVelocity*3.6);
		Assert.assertEquals(detailedPcFactorFf*pclinkLength/1000., warmEmissions.get("NMHC"), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pclink.getId(), pcVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*detailedPcFactorFf*pclinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle, pclink, pclinkLength/pcsgVelocity*3.6);
		Assert.assertEquals(avgPcFactorSg*pclinkLength/1000., warmEmissions.get("NMHC"), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pclink.getId(), pcVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorSg*pclinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent3(){
		
		//-- set up tables, event handler, parameters, module
		setUp();

		// case 3 - stop go entry in both tables, free flow entry in average table -> use average
		Id<Vehicle> dieselVehicleId = Id.create("veh 3", Vehicle.class);
		double dieselLinkLength= 20.;
		Link diesellink = createMockLink("link 3", dieselLinkLength, dieselFreeVelocity / 3.6);

		Id<VehicleType> dieselVehicleTypeId = Id.create(passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle dieselVehicle = vehFac.createVehicle(dieselVehicleId, vehFac.createVehicleType(dieselVehicleTypeId));
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink,  dieselLinkLength/dieselFreeVelocity*3.6);
		Assert.assertEquals(avgPcFactorFf*dieselLinkLength/1000., warmEmissions.get("PM"), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, diesellink.getId(), dieselVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorFf*dieselLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink,  dieselLinkLength/dieselSgVelocity*3.6);
		Assert.assertEquals(avgPcFactorSg*dieselLinkLength/1000., warmEmissions.get("PM"), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, diesellink.getId(), dieselVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorSg*dieselLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}

	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent4(){
		
		//-- set up tables, event handler, parameters, module
		setUp();

		// case 4 - data in average table
		Id<Vehicle> lpgVehicleId = Id.create("veh 4", Vehicle.class);
		double lpgLinkLength = 700.;
		Link lpglink = createMockLink("link 4", lpgLinkLength, lpgFreeVelocity / 3.6);
		Id<Link> lpgLinkId = lpglink.getId();
		Id<VehicleType> lpgVehicleTypeId = Id.create(passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle lpgVehicle = vehFac.createVehicle(lpgVehicleId, vehFac.createVehicleType(lpgVehicleTypeId));
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgVehicle, lpglink, lpgLinkLength/lpgFreeVelocity*3.6);
		Assert.assertEquals(avgPcFactorFf*lpgLinkLength/1000., warmEmissions.get("PM"), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgLinkId, lpgVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorFf*lpgLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgVehicle, lpglink, lpgLinkLength/lpgSgVelocity*3.6);
		Assert.assertEquals(avgPcFactorSg*lpgLinkLength/1000., warmEmissions.get("PM"), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgLinkId, lpgVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorSg*lpgLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test 
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent5(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 6 - data in detailed table, stop go speed zero
		// use free flow factor to calculate emissions
		Id<Vehicle> zeroVehicleId = Id.create("vehicle zero", Vehicle.class);
		double zeroLinklength = 3000.;
		Link zerolink = createMockLink("link zero", zeroLinklength, zeroFreeVelocity / 3.6);
		Id<Link> lpgLinkId = zerolink.getId();
		EmissionUtils.setHbefaRoadType(zerolink, zeroRoadCatgory);

		Id<VehicleType> zeroVehicleTypeId = Id.create(passengercar + ";"+ zeroTechnology + ";" + zeroSizeClass + ";" + zeroConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle zeroVehicle = vehFac.createVehicle(zeroVehicleId, vehFac.createVehicleType(zeroVehicleTypeId));
		
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(zeroVehicle, zerolink, 2*zeroLinklength/(zeroFreeVelocity+zeroSgVelocity)*3.6);
		Assert.assertEquals(detailedZeroFactorFf*zeroLinklength/1000., warmEmissions.get("PM"), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		
		weam.throwWarmEmissionEvent(22., lpgLinkId, zeroVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*detailedZeroFactorFf*zeroLinklength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
		
	}
	
	@Test 
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		Id<Vehicle> sgffVehicleId = Id.create("vehicle sg equals ff", Vehicle.class);
		double sgffLinklength = 4000.;
		Link sgflink = createMockLink("link sgf", sgffLinklength, sgffDetailedFfSpeed / 3.6);
		EmissionUtils.setHbefaRoadType(sgflink, sgffRoadCatgory);

		Id<VehicleType> sgffVehicleTypeId = Id.create(passengercar + ";" + sgffTechnology + ";"+ sgffSizeClass + ";"+sgffConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle sgffVehicle = vehFac.createVehicle(sgffVehicleId, vehFac.createVehicleType(sgffVehicleTypeId));
		
		//avg>ff
		boolean exceptionThrown = false;
		try{
			warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffVehicle, sgflink, .5*sgffLinklength/sgffDetailedFfSpeed*3.6);
		}
		catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assert.assertTrue("An average speed higher than the free flow speed should throw a runtime exception",exceptionThrown);
		//Assert.assertEquals(detailedSgffFactorFf*sgffLinklength/1000., warmEmissions.get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		//avg=ff=sg -> use ff factors
		warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffVehicle, sgflink, sgffLinklength/sgffDetailedFfSpeed*3.6);
		Assert.assertEquals(detailedSgffFactorFf*sgffLinklength/1000., warmEmissions.get("NO2"), MatsimTestUtils.EPSILON);
		//avg<sg -> use sg factors 
		warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffVehicle, sgflink, 2*sgffLinklength/sgffDetailedFfSpeed*3.6);
		Assert.assertEquals(detailedSgffFactorSg*sgffLinklength/1000., warmEmissions.get("NO2"), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions1(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 5 - no entry in any table - must be different to other test case's strings
		//With the bug fix to handle missing values - this test should no longer throw an error - jm oct'18
		Id<Vehicle> noeVehicleId = Id.create("veh 5", Vehicle.class);
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 5", noeLinkLength, noeFreeSpeed);

		String noeSizeClass = "size";
        String noeConcept = "concept";
        String noeTechnology = "technology";
        Id<VehicleType> noeVehicleTypeId = Id.create(passengercar + ";"+ noeTechnology + ";" + noeSizeClass + ";" + noeConcept, VehicleType.class);
        VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(noeVehicleTypeId));
		
		excep= false;
		try{
			Map<String, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noeVehicle, noelink, 1.5* noeLinkLength /noeFreeSpeed);
			weam.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertFalse(excep); excep=false;
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions2(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// no vehicle information given
		Id<Vehicle> noeVehicleId = Id.create("veh 6", Vehicle.class);
		Id<VehicleType> noeVehicleTypeId = Id.create("", VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 6", noeLinkLength, noeFreeSpeed);

		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(noeVehicleTypeId));
		
		excep= false;
		try{
			Map<String, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noeVehicle, noelink, 1.5* noeLinkLength /noeFreeSpeed);
			weam.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions3(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// empty vehicle information 
		Id<Vehicle> noeVehicleId = Id.create("veh 7", Vehicle.class);
		Id<VehicleType> noeVehicleTypeId = Id.create(";;;", VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(noeVehicleTypeId));
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 7", noeLinkLength, noeFreeSpeed);

		excep= false;
		try{
			Map<String, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noeVehicle, noelink, 1.5*noeLinkLength/noeFreeSpeed);
			weam.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions4(){
		//-- set up tables, event handler, parameters, module
		setUp();
		//  vehicle information string is 'null'
		Id<Vehicle> noeVehicleId = Id.create("veh 8", Vehicle.class);
		Id<VehicleType> noeVehicleTypeId = null;
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(noeVehicleTypeId));
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 8", noeLinkLength, noeFreeSpeed);

		excep= false;
		try{
			Map<String, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noeVehicle, noelink, 1.5*22./noeFreeSpeed);
			weam.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
		
	}
	
	@Test
	public void testCounters1(){
		setUp();
		weam.reset();
		
		/*
		 * using the same case as above - case 1 and check the counters for all possible combinations of avg, stop go and free flow speed 
		 */
		 
		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		String roadType = "0";
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create(passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		Link mockLink = createMockLink("link 1", linkLength, petrolSpeedFf / 3.6);


		// <stop&go speed
		Double travelTime = linkLength/petrolSpeedSg*1.2; 
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime*3.6);
		Assert.assertEquals(0, weam.getFractionOccurences(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(linkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// = s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength/petrolSpeedSg*3.6);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(linkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
				
		// > s&g speed, <ff speed
		// speed in km/h
		travelTime = .5 * linkLength/petrolSpeedFf *3.6 + .5* (linkLength/petrolSpeedSg)*3.6; //540 seconds
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime);
		Assert.assertEquals(2., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		Assert.assertEquals(weam.getKmCounter(), (weam.getStopGoKmCounter()+weam.getFreeFlowKmCounter()), MatsimTestUtils.EPSILON);
		//this assert is no longer relevant. More tests will be added to check the traffic situations, jm oct'18
		//Assert.assertEquals(travelTime, 3600*weam.getFreeFlowKmCounter()/petrolSpeedFf+3600*weam.getStopGoKmCounter()/petrolSpeedSg, MatsimTestUtils.EPSILON);
		weam.reset();
		
		// = ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength/petrolSpeedFf*3.6);
		Assert.assertEquals(linkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		//> ff speed
		boolean exceptionThrown = false;
		try{
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, 0.4*linkLength/petrolSpeedFf*3.6);
		}catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assert.assertTrue("An average speed higher than the free flow speed should throw a runtime exception", exceptionThrown);
		weam.reset();
		
		
	}

	@Test
	public void testCounters2(){
		setUp();
		weam.reset();
		
		// ff und sg not part of the detailed table -> use average table
		Id<Vehicle> vehicleId = Id.create("vehicle 4", Vehicle.class);
		double lpgLinkLength = 2000.*1000;
		Id<VehicleType> lpgVehicleTypeId = Id.create(passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(lpgVehicleTypeId));

		Link lpgLink = createMockLink("link zero", lpgLinkLength, lpgFreeVelocity / 3.6);

		// sub case: current speed equals free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, lpgLink, lpgLinkLength/lpgFreeVelocity*3.6);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(.0, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// sub case: current speed equals free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, lpgLink, lpgLinkLength/lpgSgVelocity*3.6);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(lpgLinkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
	}
	
	@Test
	public void testCounters3(){
		setUp();
		weam.reset();
		
		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		Id<Vehicle> pcVehicleId = Id.create("vehicle 2", Vehicle.class); 
		double pclinkLength= 20.*1000;
		Id<VehicleType> pcVehicleTypeId = Id.create(passengercar + ";"+ pcTechnology + ";"+pcSizeClass+";"+pcConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle pcVehicle = vehFac.createVehicle(pcVehicleId, vehFac.createVehicleType(pcVehicleTypeId));
		Link pclink = createMockLink("link 2", pclinkLength, pcfreeVelocity / 3.6);

		// sub case: current speed equals free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle,pclink, pclinkLength/pcfreeVelocity*3.6);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// sub case: current speed equals stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcVehicle, pclink, pclinkLength/pcsgVelocity*3.6);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(pclinkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
	}
	
	@Test
	public void testCounters4(){
	setUp();
	weam.reset();
			
	// case 3 - stop go entry in both tables, free flow entry in average table -> use average
	Id<Vehicle> dieselVehicleId = Id.create("vehicle 3", Vehicle.class);
	double dieselLinkLength= 200.*1000;
	Id<VehicleType> dieselVehicleTypeId = Id.create(passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept, VehicleType.class);
	VehiclesFactory vehFac = VehicleUtils.getFactory();
	Vehicle dieselVehicle = vehFac.createVehicle(dieselVehicleId, vehFac.createVehicleType(dieselVehicleTypeId));
	Link diesellink = createMockLink("link 3", dieselLinkLength, dieselFreeVelocity / 3.6);

		// sub case: current speed equals free flow speed
	warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink, dieselLinkLength/dieselFreeVelocity*3.6);
	Assert.assertEquals(0, weam.getFractionOccurences());
	Assert.assertEquals(dieselLinkLength/1000., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(1, weam.getFreeFlowOccurences());
	Assert.assertEquals(dieselLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0, weam.getStopGoOccurences());
	Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
	weam.reset();
	
	// sub case: current speed equals stop go speed
	warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselVehicle, diesellink, dieselLinkLength/dieselSgVelocity*3.6);
	Assert.assertEquals(0, weam.getFractionOccurences());
	Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0, weam.getFreeFlowOccurences());
	Assert.assertEquals(dieselLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(dieselLinkLength/1000., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(1, weam.getStopGoOccurences());
	Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
	weam.reset();
	}
	
	@Test
	public void testCounters5(){
		setUp();
		weam.reset();
		
		// case 1 - data in both tables -> use detailed
		// free flow velocity inconsistent -> different value in table
		Id<Vehicle> inconffVehicleId = Id.create("vehicle 7", Vehicle.class);
		double inconff = 30. * 1000;
		double inconffavgSpeed = petrolSpeedFf*2.2;
		Id<VehicleType> inconffVehicleTypeId = Id.create(passengercar + ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle inconffVehicle = vehFac.createVehicle(inconffVehicleId, vehFac.createVehicleType(inconffVehicleTypeId));
		Link inconLink = createMockLink("link incon", inconff, inconffavgSpeed / 3.6);

		// average speed equals free flow speed from table
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle,inconLink, inconff/petrolSpeedFf*3.6);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// average speed equals wrong free flow speed
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, inconff/inconffavgSpeed*3.6);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, 2*inconff/(petrolSpeedFf+petrolSpeedSg)*3.6);
	}
	
	@Test
	public void testCounters7(){
		setUp();
		weam.reset();
	
		// case 10 - data in detailed table, stop go speed > free flow speed
		Id<Vehicle> tableVehicleId = Id.create("vehicle 8", Vehicle.class);
		double tableLinkLength= 30.*1000;
		Id<VehicleType> tableVehicleTypeId = Id.create(passengercar + ";" + tableTechnology +";" + tableSizeClass+";"+tableConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle tableVehicle = vehFac.createVehicle(tableVehicleId, vehFac.createVehicleType(tableVehicleTypeId));
		Link tableLink = createMockLink("link table", tableLinkLength, tableffSpeed / 3.6);

		// ff < avg < ff+1 - handled like free flow
		Double travelTime =  tableLinkLength/(tableffSpeed+0.5)*3.6;
		weam.checkVehicleInfoAndCalculateWarmEmissions(tableVehicle, tableLink, travelTime);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(tableLinkLength/1000., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(tableLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// ff < sg < avg - handled like free flow as well - no additional test needed
		// avg < ff < sg - handled like stop go 
		weam.checkVehicleInfoAndCalculateWarmEmissions(tableVehicle, tableLink, 2* tableLinkLength/(tableffSpeed)*3.6);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(tableLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(tableLinkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
	}
	
	@Test
	public void testCounters8(){
		setUp();
		weam.reset();
		
		// test summing up of counters
		
		// case 1 - data in both tables -> use detailed
		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		String roadType = "0";
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create(passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept, VehicleType.class);
		double travelTime = linkLength/petrolSpeedSg*1.2;
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));
		Link pcLink = createMockLink("link table", linkLength, petrolSpeedFf / 3.6);

		weam.reset();
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), 1e-7);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(0., weam.getKmCounter(), 1e-7);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), 1e-7);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(0, weam.getWarmEmissionEventCounter());
	
		// < s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		// = s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, linkLength/petrolSpeedSg*3.6);
		// > s&g speed, <ff speed
		travelTime = .5 * linkLength/petrolSpeedFf *3.6 + .5* (linkLength/petrolSpeedSg)*3.6; //540 seconds
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime);
		// = ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, linkLength/petrolSpeedFf*3.6);
		//> ff speed - has been tested to throw runtime exceptions
	}
	
	@Test 
	public void rescaleWarmEmissionsTest() {
		// can not use the setUp method here because the efficiency factor is not null
		// setup ----
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();

		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		Map<String, Double> warmEmissions;

		Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds;
		hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(avgHbefaWarmTable);
		addDetailedRecordsToTestSpeedsTable(hbefaRoadTrafficSpeeds, detailedHbefaWarmTable);

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		
		double rescaleF = 1.0003;

		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setUsingVehicleTypeIdAsVehicleDescription(true);

		WarmEmissionAnalysisModuleParameter weamParameter
				= new WarmEmissionAnalysisModuleParameter(avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, ecg);
		WarmEmissionAnalysisModule weam = new WarmEmissionAnalysisModule(weamParameter , emissionEventManager, rescaleF);
		HandlerToTestEmissionAnalysisModules.reset();
		// ---- end of setup
		
		// case 3 - stop go entry in both tables, free flow entry in average table -> use average
		Id<Link> idForAvgTable = Id.create("link id avg", Link.class);
		Id<Vehicle> vehicleIdForAvgTable = Id.create("vehicle avg", Vehicle.class);
		Id<VehicleType> dieselVehicleTypeId = Id.create(passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept, VehicleType.class);
		double linkLength = 1000.;
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicleForAvgTable = vehFac.createVehicle(vehicleIdForAvgTable, vehFac.createVehicleType(dieselVehicleTypeId));
		Link dieselLink = createMockLink("link table", linkLength, dieselFreeVelocity / 3.6);

		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicleForAvgTable, dieselLink, linkLength /dieselFreeVelocity*3.6);
		weam.throwWarmEmissionEvent(10, idForAvgTable, vehicleIdForAvgTable, warmEmissions);
		
		int numberOfWarmEmissions = numberOfWarmPollutants;
		
		String message = "The expected rescaled emissions for this event are (calculated emissions * rescalefactor) = " 
				+ (numberOfWarmEmissions*avgPcFactorFf) + " * " + rescaleF + " = " +
				(numberOfWarmEmissions*avgPcFactorFf*rescaleF) + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		
		Assert.assertEquals(message, rescaleF*numberOfWarmEmissions*avgPcFactorFf, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		
	}
	
	private void setUp() {
		avgHbefaWarmTable = new HashMap<>();
		detailedHbefaWarmTable = new HashMap<>();

		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(avgHbefaWarmTable);
		addDetailedRecordsToTestSpeedsTable(hbefaRoadTrafficSpeeds, detailedHbefaWarmTable);

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
        EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setUsingVehicleTypeIdAsVehicleDescription(true);

		WarmEmissionAnalysisModuleParameter warmEmissionParameterObject = new WarmEmissionAnalysisModuleParameter(
				avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, ecg);
		weam = new WarmEmissionAnalysisModule(warmEmissionParameterObject, emissionEventManager, null);

	}

	private Link createMockLink(String linkId, double linkLength, double ffspeed) {
		Id<Link> mockLinkId = Id.createLinkId(linkId);
		Node mockNode1 = NetworkUtils.createNode(Id.createNodeId(1));
		Node mockNode2 = NetworkUtils.createNode(Id.createNodeId(2));
		Link l = NetworkUtils.createLink(mockLinkId, mockNode1, mockNode2, null, linkLength, ffspeed, 1800, 1);
		EmissionUtils.setHbefaRoadType(l, "URB");
		return l;
	}

	private void fillDetailedTable(
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {
		
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(petrolTechnology);
		vehAtt.setHbefaSizeClass(petrolSizeClass);
		vehAtt.setHbefaEmConcept(petrolConcept);
		
		HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedPetrolFactorFf); 
		detWarmFactor.setSpeed(petrolSpeedFf);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			
		}
		
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(petrolTechnology);
		vehAtt.setHbefaSizeClass(petrolSizeClass);
		vehAtt.setHbefaEmConcept(petrolConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        double detailedPetrolFactorSg = .01;
        detWarmFactor.setWarmEmissionFactor(detailedPetrolFactorSg);
		detWarmFactor.setSpeed(petrolSpeedSg);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entry for second test case "pc" -- should not be used
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcTechnology);
		vehAtt.setHbefaSizeClass(pcSizeClass);
		vehAtt.setHbefaEmConcept(pcConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        detWarmFactor.setWarmEmissionFactor(detailedPcFactorFf);
		detWarmFactor.setSpeed(pcfreeVelocity);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}

		// entry for ffOnlyTestcase
		
		vehAtt = new HbefaVehicleAttributes();
        String ffOnlyConcept = "PC-D-Euro-3";
        vehAtt.setHbefaEmConcept(ffOnlyConcept);
        String ffOnlySizeClass = ">=2L";
        vehAtt.setHbefaSizeClass(ffOnlySizeClass);
        String ffOnlyTechnology = "diesel";
        vehAtt.setHbefaTechnology(ffOnlyTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        double detailedFfOnlyFactorFf = .0000001;
        detWarmFactor.setWarmEmissionFactor(detailedFfOnlyFactorFf);
        double ffOnlyffSpeed = 120.;
        detWarmFactor.setSpeed(ffOnlyffSpeed);
		
		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entry for sgOnlyTestcase
		
		vehAtt = new HbefaVehicleAttributes();
        String sgOnlyConcept = "PC-Alternative Fuel";
        vehAtt.setHbefaEmConcept(sgOnlyConcept);
        String sgOnlySizeClass = "not specified";
        vehAtt.setHbefaSizeClass(sgOnlySizeClass);
        String sgOnlyTechnology = "bifuel CNG/petrol";
        vehAtt.setHbefaTechnology(sgOnlyTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        double detailedSgOnlyFactorSg = .00000001;
        detWarmFactor.setWarmEmissionFactor(detailedSgOnlyFactorSg);
        double sgOnlysgSpeed = 50.;
        detWarmFactor.setSpeed(sgOnlysgSpeed);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entries for table testcase
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(tableConcept);
		vehAtt.setHbefaSizeClass(tableSizeClass);
		vehAtt.setHbefaTechnology(tableTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        double detailedTableFactorFf = .11;
        detWarmFactor.setWarmEmissionFactor(detailedTableFactorFf);
		detWarmFactor.setSpeed(tableffSpeed);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        double detailedTableFactorSg = .011;
        detWarmFactor.setWarmEmissionFactor(detailedTableFactorSg);
        detWarmFactor.setSpeed(tablesgSpeed);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//entries for zero case
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(zeroConcept);
		vehAtt.setHbefaSizeClass(zeroSizeClass);
		vehAtt.setHbefaTechnology(zeroTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedZeroFactorFf);
		detWarmFactor.setSpeed(zeroFreeVelocity);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(zeroRoadCatgory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        double detailedZeroFactorSg = .00011;
        detWarmFactor.setWarmEmissionFactor(detailedZeroFactorSg);
		detWarmFactor.setSpeed(zeroSgVelocity);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(zeroRoadCatgory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entries for case sg speed = ff speed
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(sgffConcept);
		vehAtt.setHbefaSizeClass(sgffSizeClass);
		vehAtt.setHbefaTechnology(sgffTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedSgffFactorFf);
		detWarmFactor.setSpeed(sgffDetailedFfSpeed);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(sgffRoadCatgory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(sgffConcept);
		vehAtt.setHbefaSizeClass(sgffSizeClass);
		vehAtt.setHbefaTechnology(sgffTechnology);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(detailedSgffFactorSg);
		detWarmFactor.setSpeed(sgffDetailedsgSpeed);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(sgffRoadCatgory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
	}
	
	private void fillAverageTable(	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable) {
		
		// entries for first case "petrol" should not be used since there are entries in the detailed table
		// there should only average vehicle attributes in the avgHebfWarmTable jm oct'18
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		//vehAtt.setHbefaEmConcept(petrolConcept);
		//vehAtt.setHbefaSizeClass(petrolSizeClass);
		//vehAtt.setHbefaTechnology(petrolTechnology);

		HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
        double avgPetrolFactorFf = avgPcFactorFf;
        detWarmFactor.setWarmEmissionFactor(avgPetrolFactorFf);
		detWarmFactor.setSpeed(petrolSpeedFf);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// stop and go
		detWarmFactor = new HbefaWarmEmissionFactor();
        double avgPetrolFactorSg = avgPcFactorSg;
        detWarmFactor.setWarmEmissionFactor(avgPetrolFactorSg);
		detWarmFactor.setSpeed(petrolSpeedSg);

		for (String wp: pollutants){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
	}

	private void addDetailedRecordsToTestSpeedsTable(
			Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds,
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable
	) {
		detailedHbefaWarmTable.forEach((warmEmissionFactorKey, emissionFactor) -> {
			HbefaRoadVehicleCategoryKey roadVehicleCategoryKey = new HbefaRoadVehicleCategoryKey(warmEmissionFactorKey);
			HbefaTrafficSituation hbefaTrafficSituation = warmEmissionFactorKey.getHbefaTrafficSituation();
			double speed = emissionFactor.getSpeed();

			hbefaRoadTrafficSpeeds.putIfAbsent(roadVehicleCategoryKey, new HashMap<>());
			hbefaRoadTrafficSpeeds.get(roadVehicleCategoryKey).put(hbefaTrafficSituation, speed);
		});

	}


}
	

	

