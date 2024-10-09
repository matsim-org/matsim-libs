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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;

import static org.matsim.contrib.emissions.Pollutant.CO2_TOTAL;
import static org.matsim.contrib.emissions.TestWarmEmissionAnalysisModule.HBEFA_ROAD_CATEGORY;
import static org.matsim.contrib.emissions.TestWarmEmissionAnalysisModule.createMockLink;
import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod.AverageSpeed;
import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction;

/**
 * @author julia
 * */

/*
 * Case 1 - data in both tables -> use detailed
 */


/*
 * test for playground.vsp.emissions.WarmEmissionAnalysisModule
 *
 * WarmEmissionAnalysisModule (weam)
 * public methods and corresponding tests:
 * weamParameter - testWarmEmissionAnalysisParameter
 * throw warm EmissionEvent - testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * check vehicle info and calculate warm emissions -testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent*, testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions
 * get free flow occurrences - testCounters*()
 * get fraction occurrences - testCounters*()
 * get stop-go occurrences - testCounters*()
 * get km counter - testCounters*()
 * get free flow km counter - testCounters*()
 * get top go km counter - testCounters*()
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

public class TestWarmEmissionAnalysisModuleCase1{
	// This used to be one large test class, which had separate table entries for each test, but put them all into the same table.  The result was
	// difficult if not impossible to debug, and the resulting detailed table was inconsistent in the sense that it did not contain all combinations of
	// entries. -- I have now pulled this apart into 6 different test classes, this one here plus "Case1" to "Case5".  Things look ok, but given that the
	// single class before was so large that I could not fully comprehend it, there may now be errors in the ripped-apart classes.  Hopefully, over time,
	// this will help to sort things out.  kai, feb'20

	private static final HandlerToTestEmissionAnalysisModules emissionEventManager = new HandlerToTestEmissionAnalysisModules();

	private static final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ));
	private static final int leaveTime = 0;
	private static final String PASSENGER_CAR = "PASSENGER_CAR";


	// emission factors for tables - no duplicates!
	private static final Double DETAILED_PETROL_FACTOR_FF = .1;

	// vehicle information for regular test cases
	// case 1 - data in both tables -> use detailed
	private static final String PETROL_TECHNOLOGY = "PC petrol <1,4L";
	private static final String PETROL_SIZE_CLASS ="<ECE petrol (4S)";
	private static final String PETROL_CONCEPT ="<1,4L";
	private static final Double PETROL_SPEED_FF = 20.; //km/h
	private static final Double PETROL_SPEED_SG = 10.; //km/h

	private WarmEmissionAnalysisModule setUp(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod) {
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();
		TestWarmEmissionAnalysisModule.fillAverageTable( avgHbefaWarmTable );
		fillDetailedTable( detailedHbefaWarmTable );
		Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(avgHbefaWarmTable );
		TestWarmEmissionAnalysisModule.addDetailedRecordsToTestSpeedsTable( hbefaRoadTrafficSpeeds, detailedHbefaWarmTable );

		EventsManager emissionEventManager = TestWarmEmissionAnalysisModuleCase1.emissionEventManager;
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( emissionsComputationMethod );
		ecg.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		return new WarmEmissionAnalysisModule( avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, emissionEventManager, ecg );
	}


	//this test method creates a mock link and mock vehicle with a complete vehicleTypId --> detailed values are used
	//the CO2_TOTAL warm Emissions are compared to a given value --> computed by using detailed Petrol and traffic state freeflow
	//the "Sum" of all emissions is tested
	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent1(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		//-- set up tables, event handler, parameters, module
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = setUp(emissionsComputationMethod);
		// case 1 - data in both tables -> use detailed
		Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);
		double linkLength = 200.;
		Link mockLink = createMockLink("link 1", linkLength, PETROL_SPEED_FF / 3.6 );
		Id<VehicleType> vehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		Map<Pollutant, Double> warmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions( vehicle, mockLink, linkLength / PETROL_SPEED_FF * 3.6 );
		Assertions.assertEquals(DETAILED_PETROL_FACTOR_FF *linkLength/1000., warmEmissions.get( CO2_TOTAL ), MatsimTestUtils.EPSILON );

		emissionEventManager.reset();

		warmEmissionAnalysisModule.throwWarmEmissionEvent(leaveTime, mockLink.getId(), vehicleId, warmEmissions );
		Assertions.assertEquals( pollutants.size() * DETAILED_PETROL_FACTOR_FF *linkLength/1000., emissionEventManager.getSum(), MatsimTestUtils.EPSILON );

		emissionEventManager.reset();
		warmEmissions.clear();
	}


	/*
	* this test method creates a mock link and mock vehicle (petrol technology) with a complete vehicleTypId --> detailed values are used
	* the counters for all possible combinations of avg, stop-go and free flow speed are tested
	* for the cases: > s&g speed, <ff speed ; the different ComputationMethods are tested as well
	 */
	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCounters1(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = setUp(emissionsComputationMethod);

		/*
		 * using the same case as above - case 1 and check the counters for all possible combinations of avg, stop-go and free flow speed
		 */

		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		Link mockLink = createMockLink("link 1", linkLength, PETROL_SPEED_FF / 3.6 );

		// <stop&go speed
		double travelTime = linkLength / PETROL_SPEED_SG * 1.2;
		warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime * 3.6);
		Assertions.assertEquals(0, warmEmissionAnalysisModule.getFractionOccurences(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(0., warmEmissionAnalysisModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(0, warmEmissionAnalysisModule.getFreeFlowOccurences());
		Assertions.assertEquals(linkLength / 1000, warmEmissionAnalysisModule.getKmCounter(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(linkLength / 1000, warmEmissionAnalysisModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(1, warmEmissionAnalysisModule.getStopGoOccurences());
		Assertions.assertEquals(1, warmEmissionAnalysisModule.getWarmEmissionEventCounter());
		warmEmissionAnalysisModule.reset();

		// = s&g speed
		warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength / PETROL_SPEED_SG * 3.6);
		Assertions.assertEquals(0, warmEmissionAnalysisModule.getFractionOccurences() );
		Assertions.assertEquals(0., warmEmissionAnalysisModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, warmEmissionAnalysisModule.getFreeFlowOccurences() );
		Assertions.assertEquals(linkLength/1000, warmEmissionAnalysisModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(linkLength/1000, warmEmissionAnalysisModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, warmEmissionAnalysisModule.getStopGoOccurences() );
		Assertions.assertEquals(1, warmEmissionAnalysisModule.getWarmEmissionEventCounter() );
		warmEmissionAnalysisModule.reset();

		// > s&g speed, <ff speed
		// speed in km/h
		travelTime = .5 * linkLength/ PETROL_SPEED_FF *3.6 + .5* (linkLength/ PETROL_SPEED_SG)*3.6; //540 seconds
		warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime);
		switch( emissionsComputationMethod ) {
			// the following may be different for the wrong reasons, see comments in the ...Event2 method above.  kai, jan'20
			case StopAndGoFraction:
				Assertions.assertEquals(1., warmEmissionAnalysisModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
				Assertions.assertEquals(0, warmEmissionAnalysisModule.getFreeFlowOccurences() );
				Assertions.assertEquals(1., warmEmissionAnalysisModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
				break;
			case AverageSpeed:
				Assertions.assertEquals(2., warmEmissionAnalysisModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
				Assertions.assertEquals(1, warmEmissionAnalysisModule.getFreeFlowOccurences() );
				Assertions.assertEquals(0., warmEmissionAnalysisModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
		}
		Assertions.assertEquals(linkLength/1000, warmEmissionAnalysisModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, warmEmissionAnalysisModule.getStopGoOccurences() );
		Assertions.assertEquals(1, warmEmissionAnalysisModule.getWarmEmissionEventCounter() );
		Assertions.assertEquals( warmEmissionAnalysisModule.getKmCounter(), (warmEmissionAnalysisModule.getStopGoKmCounter()+ warmEmissionAnalysisModule.getFreeFlowKmCounter()), MatsimTestUtils.EPSILON );
		warmEmissionAnalysisModule.reset();

		// = ff speed
		warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength / PETROL_SPEED_FF * 3.6);
		Assertions.assertEquals(0, warmEmissionAnalysisModule.getFractionOccurences() );
		Assertions.assertEquals(linkLength/1000, warmEmissionAnalysisModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, warmEmissionAnalysisModule.getFreeFlowOccurences() );
		Assertions.assertEquals(linkLength/1000, warmEmissionAnalysisModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0., warmEmissionAnalysisModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, warmEmissionAnalysisModule.getStopGoOccurences() );
		Assertions.assertEquals(1, warmEmissionAnalysisModule.getWarmEmissionEventCounter() );
		warmEmissionAnalysisModule.reset();

		//> ff speed
		boolean exceptionThrown = false;
		try{
			warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, 0.4 * linkLength / PETROL_SPEED_FF * 3.6);
		}catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assertions.assertTrue(exceptionThrown, "An average speed higher than the free flow speed should throw a runtime exception");
		warmEmissionAnalysisModule.reset();
	}


	/*
	 * this test method creates an incoff mock link and incoff mock vehicle (petrol technology) with a complete vehicleTypId --> detailed values are used
	 * for the computationMethod "Stop and Go" and "averageSpeed" the free flow occurrences are tested
	 * the counters (StopGoOccurrences, KmCounter, WarmEmissionEventCounter) are tested
	 * for the case average speed equals wrong free flow speed the counters are tested
	 */

	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCounters5(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){

		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);

		// case 1 - data in both tables -> use detailed
		// free flow velocity inconsistent -> different value in table
		Id<Vehicle> inconffVehicleId = Id.create("vehicle 7", Vehicle.class);
		double inconff = 30. * 1000;
		double inconffavgSpeed = PETROL_SPEED_FF *2.2;
		Id<VehicleType> inconffVehicleTypeId = Id.create(
				PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle inconffVehicle = vehFac.createVehicle(inconffVehicleId, vehFac.createVehicleType(inconffVehicleTypeId));
		Link inconLink = createMockLink("link incon", inconff, inconffavgSpeed / 3.6 );

		// average speed equals free flow speed from table
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, inconff / PETROL_SPEED_FF * 3.6);

		switch( emissionsComputationMethod ) {
			case StopAndGoFraction:
				Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
				break;
			case AverageSpeed:
				Assertions.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
		}

		Assertions.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// average speed equals wrong free flow speed
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, inconff / inconffavgSpeed * 3.6);
		Assertions.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );

		//@KMT is there the need for adding a test here?
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, 2 * inconff / (PETROL_SPEED_FF + PETROL_SPEED_SG) * 3.6);
	}


	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCounters1fractional(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){

		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);
		emissionsModule.getEcg().setEmissionsComputationMethod(StopAndGoFraction );

		// yyyyyy !!!!!!

		/*
		 * using the same case as above - case 1 and check the counters for all possible combinations of avg, stop-go and free flow speed
		 */

		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		Link mockLink = createMockLink("link 1", linkLength, PETROL_SPEED_FF / 3.6 );

		// <stop&go speed
		double travelTime = linkLength/ PETROL_SPEED_SG *1.2;
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime * 3.6);
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(linkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// = s&g speed
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength / PETROL_SPEED_SG * 3.6);
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(linkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// > s&g speed, <ff speed
		// speed in km/h
		travelTime = .5 * linkLength/ PETROL_SPEED_FF *3.6 + .5* (linkLength/ PETROL_SPEED_SG)*3.6; //540 seconds
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime);
		Assertions.assertEquals(1, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(1., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		Assertions.assertEquals( emissionsModule.getKmCounter(), (emissionsModule.getStopGoKmCounter()+ emissionsModule.getFreeFlowKmCounter()), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(travelTime, 3600* emissionsModule.getFreeFlowKmCounter()/ PETROL_SPEED_FF +3600* emissionsModule.getStopGoKmCounter()/ PETROL_SPEED_SG, MatsimTestUtils.EPSILON );
		emissionsModule.reset();

		// = ff speed
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength / PETROL_SPEED_FF * 3.6);
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(linkLength/1000, emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		//> ff speed
		boolean exceptionThrown = false;
		try{
			emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, 0.4 * linkLength / PETROL_SPEED_FF * 3.6);
		}catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assertions.assertTrue(exceptionThrown, "An average speed higher than the free flow speed should throw a runtime exception");

		emissionsModule.reset();
		emissionsModule.getEcg().setEmissionsComputationMethod(AverageSpeed );
		//@KMT it seems to me that copying the counters from above and changing the expected values??
		// yyyyyy !!!!!!

	}


	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCounters6(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){

		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);
		emissionsModule.getEcg().setEmissionsComputationMethod(StopAndGoFraction );

		// case 1 - data in both tables -> use detailed
		// free flow velocity inconsistent -> different value in table
		Id<Vehicle> inconffVehicleId = Id.create("vehicle 7", Vehicle.class);
		double inconff = 30. * 1000;
		double inconffavgSpeed = PETROL_SPEED_FF *2.2;
		Id<VehicleType> inconffVehicleTypeId = Id.create(PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle inconffVehicle = vehFac.createVehicle(inconffVehicleId, vehFac.createVehicleType(inconffVehicleTypeId));
		Link inconLink = createMockLink("link incon", inconff, inconffavgSpeed / 3.6 );

		// average speed equals free flow speed from table
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink,
				inconff / PETROL_SPEED_FF * 3.6);
		Assertions.assertEquals(1, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// average speed equals wrong free flow speed
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, inconff / inconffavgSpeed * 3.6);
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );

		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, 2 * inconff / (PETROL_SPEED_FF + PETROL_SPEED_SG) * 3.6);
	}

	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	void testCounters8(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){

		WarmEmissionAnalysisModule emissionsModule = setUp(emissionsComputationMethod);

		// test summing up of counters

		// case 1 - data in both tables -> use detailed
		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		double travelTime = linkLength/ PETROL_SPEED_SG *1.2;
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));
		Link pcLink = createMockLink("link table", linkLength, PETROL_SPEED_FF / 3.6 );

		emissionsModule.reset();
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), 1e-7 );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(0., emissionsModule.getKmCounter(), 1e-7 );
		Assertions.assertEquals(0., emissionsModule.getStopGoKmCounter(), 1e-7 );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(0, emissionsModule.getWarmEmissionEventCounter() );

		// < s&g speed
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime * 3.6);
		// = s&g speed
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, linkLength / PETROL_SPEED_SG * 3.6);
		// > s&g speed, <ff speed
		travelTime = .5 * linkLength/ PETROL_SPEED_FF *3.6 + .5* (linkLength/ PETROL_SPEED_SG)*3.6; //540 seconds
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime);
		// = ff speed
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, linkLength / PETROL_SPEED_FF * 3.6);
		//> ff speed - has been tested to throw runtime exceptions
	}



	private static void fillDetailedTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable ) {

		// petrol free-flow:
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology(PETROL_TECHNOLOGY);
			vehAtt.setHbefaSizeClass(PETROL_SIZE_CLASS);
			vehAtt.setHbefaEmConcept(PETROL_CONCEPT);

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(DETAILED_PETROL_FACTOR_FF, PETROL_SPEED_FF);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.FREEFLOW);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);

			}
		}

		// petrol stop-go:
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology(PETROL_TECHNOLOGY);
			vehAtt.setHbefaSizeClass(PETROL_SIZE_CLASS);
			vehAtt.setHbefaEmConcept(PETROL_CONCEPT);

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(.01, PETROL_SPEED_SG);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.STOPANDGO);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}

	}

}




