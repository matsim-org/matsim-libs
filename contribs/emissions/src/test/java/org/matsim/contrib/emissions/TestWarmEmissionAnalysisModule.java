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

import junit.framework.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule.WarmEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.types.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;


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
	
	private final int numberOfWarmPollutants= WarmPollutant.values().length;
	private final String hbefaRoadCategory = "URB";
    private final int roadType =0;
	private final int leaveTime = 0;
	private boolean excep =false;
	private final String passengercar= "PASSENGER_CAR";

    private Map<Integer, String> roadTypeMapping;
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable;
	private Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
    private WarmEmissionAnalysisModule weam;
	private Map<WarmPollutant, Double> warmEmissions;
	
	// saturated and heavy not used so far -> not tested
    private final HbefaTrafficSituation trafficSituationff = HbefaTrafficSituation.FREEFLOW;
	private final HbefaTrafficSituation trafficSituationsg = HbefaTrafficSituation.STOPANDGO;
	
	// emission factors for tables - no dublicates!
    private final Double detailedPetrolFactorFf = .1;
    private final Double detailedZeroFactorFf  =  .0011;
    private final Double detailedSgffFactorFf =   .000011;
	private final Double detailedSgffFactorSg = 	.0000011;
	private final Double avgPcFactorFf = 1.;
	private final Double avgPcFactorSg= 10.;
	private final Double avgDieselFactorFf = 100.;
	private final Double avgDieselFactorSg = 1000.;
	private final Double avgLpgFactorFf = 10000.;
	private final Double avgLpgFactorSg = 100000.;

    // vehicle information for regular test cases
	// case 1 - data in both tables -> use detailed
    private final String 	petrolTechnology = "PC petrol <1,4L";
    private final String petrolSizeClass ="<ECE petrol (4S)";
    private final String petrolConcept ="<1,4L";
	private final Double petrolSpeedFf = 20.;
    private final Double petrolSpeedSg = 10.;
	// case 2 - free flow entry in both tables, stop go entry in average table -> use average
    private final String 	pcTechnology = "PC petrol <1,4L <ECE";
    private final String pcSizeClass = "petrol (4S)";
    private final String pcConcept = "<1,4L";
	private final Double pcfreeVelocity = 50.;
    private final Double pcsgVelocity= 10.;
	// case 3 - stop go entry in both tables, free flow entry in average table -> use average
    private final String 	dieselTechnology = "PC diesel";
    private final String dieselSizeClass = "diesel";
    private final String dieselConcept = ">=2L";
	private final Double dieselFreeVelocity = 100.;
    private final Double dieselSgVelocity = 30.;
	// case 4 - data in average table
    private final String 	lpgTechnology = "PC LPG Euro-4";
    private final String lpgSizeClass = "LPG";
    private final String lpgConcept = "not specified";
	private final Double lpgFreeVelocity = 100.;
    private final Double lpgSgVelocity = 35.;
    private final Double noeFreeSpeed=100.;
	// case 6 - data in detailed table, stop go speed zero
    private final String zeroTechnology = "zero technology";
    private final String zeroConcept = "zero concept";
    private final String zeroSizeClass = "zero size class";
	private final Double zeroFreeVelocity = 100.;
    private final Double zeroSgVelocity = 0.;
	// case 7 - data in detailed table, stop go speed = free flow speed
    private final String sgffTechnology = "sg ff technology";
    private final String sgffConcept = "sg ff concept";
    private final String sgffSizeClass = "sg ff size class";
	private final Double sgffDetailedFfSpeed = 44.;
    // case 10 - data in detailed table, stop go speed > free flow speed
    private final String tableTechnology = "petrol (4S)";
    private final String tableSizeClass= "<1,4L";
    private final String tableConcept = "PC-P-Euro-0";
	private final Double tableffSpeed = 30.;

    @Test
	public void testWarmEmissionAnalysisParameter(){
		setUp();
		WarmEmissionAnalysisModuleParameter weamp = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, null);
		Assert.assertEquals(weamp.getClass(), WarmEmissionAnalysisModuleParameter.class);
		weamp = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, null, detailedHbefaWarmTable);
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
		Id<Link> personId = Id.create("person 1", Link.class);    // !!!
		Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);		
		Double linkLength = 200.; 
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, linkLength, linkLength/petrolSpeedFf*3.6, vehicleInformation);
		Assert.assertEquals(detailedPetrolFactorFf*linkLength/1000., warmEmissions.get(WarmPollutant.CO2_TOTAL), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		
		weam.throwWarmEmissionEvent(leaveTime, personId, vehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*detailedPetrolFactorFf*linkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent2(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 2 - free flow entry in both tables, stop go entry in average table -> use average
		Id<Link> pcPersonId = Id.create("person 2", Link.class);
		Id<Vehicle> pcVehicleId = Id.create("veh 2", Vehicle.class);
		Double pclinkLength= 100.;
		String pcVehicleInformation = passengercar + ";"+ pcTechnology + ";"+pcSizeClass+";"+pcConcept;
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcfreeVelocity*3.6, pcVehicleInformation);
		Assert.assertEquals(avgPcFactorFf*pclinkLength/1000., warmEmissions.get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pcPersonId, pcVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorFf*pclinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcsgVelocity*3.6, pcVehicleInformation);
		Assert.assertEquals(avgPcFactorSg*pclinkLength/1000., warmEmissions.get(WarmPollutant.NMHC), MatsimTestUtils.EPSILON);
		weam.throwWarmEmissionEvent(leaveTime, pcPersonId, pcVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgPcFactorSg*pclinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent3(){
		
		//-- set up tables, event handler, parameters, module
		setUp();

		// case 3 - stop go entry in both tables, free flow entry in average table -> use average
		Id<Link> dieselPersonId = Id.create("person 3", Link.class);
		Id<Vehicle> dieselVehicleId = Id.create("veh 3", Vehicle.class);
		Double dieselLinkLength= 20.;
		String dieselVehicleInformation = passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept;
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselFreeVelocity*3.6, dieselVehicleInformation);
		Assert.assertEquals(avgDieselFactorFf*dieselLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, dieselPersonId, dieselVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgDieselFactorFf*dieselLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselSgVelocity*3.6, dieselVehicleInformation);
		Assert.assertEquals(avgDieselFactorSg*dieselLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, dieselPersonId, dieselVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgDieselFactorSg*dieselLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}

	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent4(){
		
		//-- set up tables, event handler, parameters, module
		setUp();

		// case 4 - data in average table
		Id<Link> lpgPersonId = Id.create("person4", Link.class);
		Id<Vehicle> lpgVehicleId = Id.create("veh 4", Vehicle.class);
		Double lpgLinkLength = 700.;
		String lpgVehicleInformation = passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept;		
		
		// sub case avg speed = free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgPersonId, roadType, lpgFreeVelocity/3.6, lpgLinkLength, lpgLinkLength/lpgFreeVelocity*3.6, lpgVehicleInformation);
		Assert.assertEquals(avgLpgFactorFf*lpgLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgPersonId, lpgVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgLpgFactorFf*lpgLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();

		// sub case avg speed = stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(lpgPersonId, roadType, lpgFreeVelocity/3.6, lpgLinkLength, lpgLinkLength/lpgSgVelocity*3.6, lpgVehicleInformation);
		Assert.assertEquals(avgLpgFactorSg*lpgLinkLength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		weam.throwWarmEmissionEvent(leaveTime, lpgPersonId, lpgVehicleId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*avgLpgFactorSg*lpgLinkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
	}
	
	@Test 
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent5(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 6 - data in detailed table, stop go speed zero
		// use free flow factor to calculate emissions
		Id<Vehicle> zeroPersonId = Id.create("person zero", Vehicle.class);
		Id<Link> zeroLinkId = Id.create("link zero", Link.class);
		Double zeroLinklength = 3000.;
		String zeroVehicleInformation = passengercar + ";"+ zeroTechnology + ";" + zeroSizeClass + ";" + zeroConcept;
		
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(zeroPersonId, roadType, zeroFreeVelocity/3.6, zeroLinklength, 2*zeroLinklength/(zeroFreeVelocity+zeroSgVelocity)*3.6, zeroVehicleInformation);
		Assert.assertEquals(detailedZeroFactorFf*zeroLinklength/1000., warmEmissions.get(WarmPollutant.PM), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset();
		
		weam.throwWarmEmissionEvent(22., zeroLinkId, zeroPersonId, warmEmissions);
		Assert.assertEquals(numberOfWarmPollutants*detailedZeroFactorFf*zeroLinklength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		HandlerToTestEmissionAnalysisModules.reset(); warmEmissions.clear();
		
	}
	
	@Test 
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		Id<Person> sgffPersonId = Id.create("person sg equals ff", Person.class);
		Double sgffLinklength = 4000.;
		String sgffVehicleInformaition = passengercar + ";" + sgffTechnology + ";"+ sgffSizeClass + ";"+sgffConcept;
		
		//avg>ff
		boolean exceptionThrown = false;
		try{
			warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffPersonId, roadType, sgffDetailedFfSpeed/3.6, sgffLinklength, .5*sgffLinklength/sgffDetailedFfSpeed*3.6, sgffVehicleInformaition);
		}
		catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assert.assertTrue("An average speed higher than the free flow speed should throw a runtime exception",exceptionThrown);
		//Assert.assertEquals(detailedSgffFactorFf*sgffLinklength/1000., warmEmissions.get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		//avg=ff=sg -> use ff factors
		warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffPersonId, roadType, sgffDetailedFfSpeed/3.6, sgffLinklength, sgffLinklength/sgffDetailedFfSpeed*3.6, sgffVehicleInformaition);
		Assert.assertEquals(detailedSgffFactorFf*sgffLinklength/1000., warmEmissions.get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
		//avg<sg -> use sg factors 
		warmEmissions= weam.checkVehicleInfoAndCalculateWarmEmissions(sgffPersonId, roadType, sgffDetailedFfSpeed/3.6, sgffLinklength, 2*sgffLinklength/sgffDetailedFfSpeed*3.6, sgffVehicleInformaition);
		Assert.assertEquals(detailedSgffFactorSg*sgffLinklength/1000., warmEmissions.get(WarmPollutant.NO2), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions1(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// case 5 - no entry in any table - must be different to other test case's strings	
		Id<Link> noePersonId = Id.create("person 5", Link.class);
		Id<Vehicle> noeVehicleId = Id.create("veh 5", Vehicle.class);
        String noeSizeClass = "size";
        String noeConcept = "concept";
        String noeTechnology = "technology";
        String noeVehicleInformation = passengercar + ";"+ noeTechnology + ";" + noeSizeClass + ";" + noeConcept;
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
		}catch(Exception e){
			excep = true;
		}
		Assert.assertTrue(excep); excep=false;
	}
	
	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions2(){
		//-- set up tables, event handler, parameters, module
		setUp();
		
		// no vehicle information given
		Id<Link> noePersonId = Id.create("person 6", Link.class);
		Id<Vehicle> noeVehicleId = Id.create("veh 6", Vehicle.class);
		String noeVehicleInformation = "";
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
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
		Id<Link> noePersonId = Id.create("person 7", Link.class);
		Id<Vehicle> noeVehicleId = Id.create("veh 7", Vehicle.class);
		String noeVehicleInformation = ";;;";
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
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
		Id<Link> noePersonId = Id.create("person 8", Link.class);
		Id<Vehicle> noeVehicleId = Id.create("veh 8", Vehicle.class);
		String noeVehicleInformation = null;
		
		excep= false;
		try{
			Map<WarmPollutant, Double> warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions
					(noePersonId, roadType, noeFreeSpeed, 22., 1.5*22./noeFreeSpeed, noeVehicleInformation);
			weam.throwWarmEmissionEvent(10., noePersonId, noeVehicleId, warmEmissions);
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
		 
		Id<Person> personId = Id.create("person 1", Person.class);
		Integer roadType = 0;
		Double linkLength = 2*1000.; //in meter
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;

		// <stop&go speed
		Double travelTime = linkLength/petrolSpeedSg*1.2; 
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime*3.6, vehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(linkLength/1000, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// = s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedSg*3.6, vehicleInformation);
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
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime , vehicleInformation);
		Assert.assertEquals(1, weam.getFractionOccurences());
		Assert.assertEquals(1., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(linkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		Assert.assertEquals(weam.getKmCounter(), (weam.getStopGoKmCounter()+weam.getFreeFlowKmCounter()), MatsimTestUtils.EPSILON);
		Assert.assertEquals(travelTime, 3600*weam.getFreeFlowKmCounter()/petrolSpeedFf+3600*weam.getStopGoKmCounter()/petrolSpeedSg, MatsimTestUtils.EPSILON);
		weam.reset();
		
		// = ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedFf*3.6, vehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
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
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, 0.4*linkLength/petrolSpeedFf*3.6, vehicleInformation);
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
		Id<Person> personId = Id.create("person4", Person.class);
		Double lpgLinkLength = 2000.*1000;
		String lpgVehicleInformation = passengercar + ";"+ lpgTechnology+";"+lpgSizeClass+";"+lpgConcept;
		
		// sub case: current speed equals free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, lpgFreeVelocity/3.6, 
				lpgLinkLength, lpgLinkLength/lpgFreeVelocity*3.6, lpgVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(lpgLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(.0, weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// sub case: current speed equals free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, lpgFreeVelocity/3.6, 
				lpgLinkLength, lpgLinkLength/lpgSgVelocity*3.6, lpgVehicleInformation);
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
		Id<Person> pcPersonId = Id.create("person 2", Person.class); 
		Double pclinkLength= 20.*1000;
		String pcVehicleInformation = passengercar + ";"+ pcTechnology + ";"+pcSizeClass+";"+pcConcept;
		
		// sub case: current speed equals free flow speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcfreeVelocity*3.6, pcVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(pclinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// sub case: current speed equals stop go speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(pcPersonId, roadType, pcfreeVelocity/3.6, pclinkLength, pclinkLength/pcsgVelocity*3.6, pcVehicleInformation);
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
	Id<Person> dieselPersonId = Id.create("person 3", Person.class);
	Double dieselLinkLength= 200.*1000;
	String dieselVehicleInformation = passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept;
	
	// sub case: current speed equals free flow speed
	warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselFreeVelocity*3.6, dieselVehicleInformation);
	Assert.assertEquals(0, weam.getFractionOccurences());
	Assert.assertEquals(dieselLinkLength/1000., weam.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(1, weam.getFreeFlowOccurences());
	Assert.assertEquals(dieselLinkLength/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0., weam.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
	Assert.assertEquals(0, weam.getStopGoOccurences());
	Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
	weam.reset();
	
	// sub case: current speed equals stop go speed
	warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(dieselPersonId, roadType, dieselFreeVelocity/3.6, dieselLinkLength, dieselLinkLength/dieselSgVelocity*3.6, dieselVehicleInformation);
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
		Id<Person> inconffPersonId = Id.create("person 7", Person.class);
		Double inconff = 30. * 1000;
		Double inconffavgSpeed = petrolSpeedFf*2.2;
		String inconffVehicleInformation = passengercar + ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		
		// average speed equals free flow speed from table
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, inconff/petrolSpeedFf*3.6, inconffVehicleInformation);
		Assert.assertEquals(1, weam.getFractionOccurences());
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		weam.reset();
		
		// average speed equals wrong free flow speed
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, inconff/inconffavgSpeed*3.6, inconffVehicleInformation);
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(1, weam.getFreeFlowOccurences());
		Assert.assertEquals(inconff/1000, weam.getKmCounter(), MatsimTestUtils.EPSILON);
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(1, weam.getWarmEmissionEventCounter());
		
		warmEmissions =weam.checkVehicleInfoAndCalculateWarmEmissions(inconffPersonId, roadType, inconffavgSpeed/3.6, inconff, 2*inconff/(petrolSpeedFf+petrolSpeedSg)*3.6, inconffVehicleInformation);
	}
	
	@Test
	public void testCounters7(){
		setUp();
		weam.reset();
	
		// case 10 - data in detailed table, stop go speed > free flow speed
		Id<Person> tablePersonId = Id.create("person 8", Person.class);
		Double tableLinkLength= 30.*1000;
		String tableVehicleInformation = passengercar + ";" + tableTechnology +";" + tableSizeClass+";"+tableConcept;
		 
		// ff < avg < ff+1 - handled like free flow
		Double travelTime =  tableLinkLength/(tableffSpeed+0.5)*3.6;
		weam.checkVehicleInfoAndCalculateWarmEmissions(tablePersonId, roadType, tableffSpeed/3.6, tableLinkLength, travelTime, tableVehicleInformation);
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
		weam.checkVehicleInfoAndCalculateWarmEmissions(tablePersonId, roadType, tableffSpeed/3.6, tableLinkLength, 2* tableLinkLength/(tableffSpeed)*3.6, tableVehicleInformation);
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
		Id<Person> personId = Id.create("person 1", Person.class);
		Integer roadType = 0;
		Double linkLength = 2*1000.; //in meter
		String vehicleInformation = passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept;
		Double travelTime = linkLength/petrolSpeedSg*1.2; 
		
		weam.reset();
		Assert.assertEquals(0, weam.getFractionOccurences());
		Assert.assertEquals(0., weam.getFreeFlowKmCounter());
		Assert.assertEquals(0, weam.getFreeFlowOccurences());
		Assert.assertEquals(0., weam.getKmCounter());
		Assert.assertEquals(0., weam.getStopGoKmCounter());
		Assert.assertEquals(0, weam.getStopGoOccurences());
		Assert.assertEquals(0, weam.getWarmEmissionEventCounter());
	
		// < s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime*3.6, vehicleInformation);
		// = s&g speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedSg*3.6, vehicleInformation);
		// > s&g speed, <ff speed
		travelTime = .5 * linkLength/petrolSpeedFf *3.6 + .5* (linkLength/petrolSpeedSg)*3.6; //540 seconds
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, travelTime , vehicleInformation);
		// = ff speed
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personId, roadType, petrolSpeedFf/3.6, 
				linkLength, linkLength/petrolSpeedFf*3.6, vehicleInformation);
		//> ff speed - has been tested to throw runtime exceptions
	}
	
	@Test 
	public void rescaleWarmEmissionsTest() {
		// can not use the setUp method here because the efficiency factor is not null
		
		// setup ----
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();
		Map<Integer, String> roadTypeMapping = new HashMap<>();
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);
		fillRoadTypeMapping(roadTypeMapping);
		Map<WarmPollutant, Double> warmEmissions;
		
		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		
		Double rescaleF = 1.0003;
		
		WarmEmissionAnalysisModuleParameter weamParameter = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		WarmEmissionAnalysisModule weam = new WarmEmissionAnalysisModule(weamParameter , emissionEventManager, rescaleF);
		HandlerToTestEmissionAnalysisModules.reset();
		// ---- end of setup
		
		// case 3 - stop go entry in both tables, free flow entry in average table -> use average
		Id<Link> idForAvgTable = Id.create("link id avg", Link.class);
		Id<Vehicle> personIdForAvgTable = Id.create("person avg", Vehicle.class);
		String dieselVehicleInformation = passengercar +";"+ dieselTechnology+ ";"+ dieselSizeClass+";"+dieselConcept;
		
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(personIdForAvgTable, roadType, dieselFreeVelocity/3.6, 1000., 1000./dieselFreeVelocity*3.6, dieselVehicleInformation);
		weam.throwWarmEmissionEvent(10, idForAvgTable, personIdForAvgTable, warmEmissions);
		
		int numberOfWarmEmissions = WarmPollutant.values().length;
		
		String message = "The expected rescaled emissions for this event are (calculated emissions * rescalefactor) = " 
				+ (numberOfWarmEmissions*avgDieselFactorFf) + " * " + rescaleF + " = " +
				(numberOfWarmEmissions*avgDieselFactorFf*rescaleF) + " but were " + HandlerToTestEmissionAnalysisModules.getSum();
		
		Assert.assertEquals(message, rescaleF*numberOfWarmEmissions*avgDieselFactorFf, HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON);
		
	}
	
	private void setUp() {
		roadTypeMapping = new HashMap<>();
		avgHbefaWarmTable = new HashMap<>();
		detailedHbefaWarmTable = new HashMap<>();
		
		fillRoadTypeMapping(roadTypeMapping);
		fillAverageTable(avgHbefaWarmTable);
		fillDetailedTable(detailedHbefaWarmTable);

        EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
        WarmEmissionAnalysisModuleParameter warmEmissionParameterObject = new WarmEmissionAnalysisModuleParameter(roadTypeMapping, avgHbefaWarmTable, detailedHbefaWarmTable);
		weam = new WarmEmissionAnalysisModule(warmEmissionParameterObject, emissionEventManager, null);
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

		for (WarmPollutant wp: WarmPollutant.values()){
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
        Double detailedPetrolFactorSg = .01;
        detWarmFactor.setWarmEmissionFactor(detailedPetrolFactorSg);
		detWarmFactor.setSpeed(petrolSpeedSg);

		for (WarmPollutant wp: WarmPollutant.values()){
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
        Double detailedPcFactorFf = .0001;
        detWarmFactor.setWarmEmissionFactor(detailedPcFactorFf);
		detWarmFactor.setSpeed(pcfreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entry for third test case "diesel"
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(dieselTechnology);
		vehAtt.setHbefaSizeClass(dieselSizeClass);
		vehAtt.setHbefaEmConcept(dieselConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        Double detailedDieselFactorSg = .00001;
        detWarmFactor.setWarmEmissionFactor(detailedDieselFactorSg);
		detWarmFactor.setSpeed(dieselSgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
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
        Double detailedFfOnlyFactorFf = .0000001;
        detWarmFactor.setWarmEmissionFactor(detailedFfOnlyFactorFf);
        Double ffOnlyffSpeed = 120.;
        detWarmFactor.setSpeed(ffOnlyffSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
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
        Double detailedSgOnlyFactorSg = .00000001;
        detWarmFactor.setWarmEmissionFactor(detailedSgOnlyFactorSg);
        Double sgOnlysgSpeed = 50.;
        detWarmFactor.setSpeed(sgOnlysgSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
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
        Double detailedTableFactorFf = .11;
        detWarmFactor.setWarmEmissionFactor(detailedTableFactorFf);
		detWarmFactor.setSpeed(tableffSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        Double detailedTableFactorSg = .011;
        detWarmFactor.setWarmEmissionFactor(detailedTableFactorSg);
        Double tablesgSpeed = 55.;
        detWarmFactor.setSpeed(tablesgSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
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
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		detWarmFactor = new HbefaWarmEmissionFactor();
        Double detailedZeroFactorSg = .00011;
        detWarmFactor.setWarmEmissionFactor(detailedZeroFactorSg);
		detWarmFactor.setSpeed(zeroSgVelocity);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
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
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
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
		detWarmFactor.setSpeed(sgffDetailedFfSpeed);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
	}

	private void fillAverageTable(	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable) {
		
		// entries for first case "petrol" should not be used since there are entries in the detailed table
		// free flow
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaEmConcept(petrolConcept);
		vehAtt.setHbefaSizeClass(petrolSizeClass);
		vehAtt.setHbefaTechnology(petrolTechnology);
		
		HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
        Double avgPetrolFactorFf = 1000000.;
        detWarmFactor.setWarmEmissionFactor(avgPetrolFactorFf);
		detWarmFactor.setSpeed(petrolSpeedFf);
		
		for (WarmPollutant wp: WarmPollutant.values()){
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
        Double avgPetrolFactorSg = 10000000.;
        detWarmFactor.setWarmEmissionFactor(avgPetrolFactorSg);
		detWarmFactor.setSpeed(petrolSpeedSg);
		
		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//entry for second test case "pc"
		// free flow
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcTechnology);
		vehAtt.setHbefaSizeClass(pcSizeClass);
		vehAtt.setHbefaEmConcept(pcConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgPcFactorFf); 
		detWarmFactor.setSpeed(pcfreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//stop and go
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(pcTechnology);
		vehAtt.setHbefaSizeClass(pcSizeClass);
		vehAtt.setHbefaEmConcept(pcConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgPcFactorSg); 
		detWarmFactor.setSpeed(pcsgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		// entries for third test case "diesel"
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(dieselTechnology);
		vehAtt.setHbefaSizeClass(dieselSizeClass);
		vehAtt.setHbefaEmConcept(dieselConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgDieselFactorFf); 
		detWarmFactor.setSpeed(dieselFreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(dieselTechnology);
		vehAtt.setHbefaSizeClass(dieselSizeClass);
		vehAtt.setHbefaEmConcept(dieselConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgDieselFactorSg); 
		detWarmFactor.setSpeed(dieselSgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		//entries for fourth test case "lpg"
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(lpgTechnology);
		vehAtt.setHbefaSizeClass(lpgSizeClass);
		vehAtt.setHbefaEmConcept(lpgConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgLpgFactorFf); 
		detWarmFactor.setSpeed(lpgFreeVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationff);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology(lpgTechnology);
		vehAtt.setHbefaSizeClass(lpgSizeClass);
		vehAtt.setHbefaEmConcept(lpgConcept);
		
		detWarmFactor = new HbefaWarmEmissionFactor();
		detWarmFactor.setWarmEmissionFactor(avgLpgFactorSg); 
		detWarmFactor.setSpeed(lpgSgVelocity);

		for (WarmPollutant wp: WarmPollutant.values()){
			HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();	
			detWarmKey.setHbefaComponent(wp);
			detWarmKey.setHbefaRoadCategory(hbefaRoadCategory);
			detWarmKey.setHbefaTrafficSituation(trafficSituationsg);
			detWarmKey.setHbefaVehicleAttributes(vehAtt);
			detWarmKey.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
			avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
		}
		
		
	}

	private void fillRoadTypeMapping(Map<Integer, String> roadTypeMapping) {
		roadTypeMapping.put(0, hbefaRoadCategory);
		
	}		
	
}
	

	

