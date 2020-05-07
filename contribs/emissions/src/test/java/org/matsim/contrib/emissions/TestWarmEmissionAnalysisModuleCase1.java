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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

import static org.matsim.contrib.emissions.Pollutant.*;
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

@RunWith(Parameterized.class)
public class TestWarmEmissionAnalysisModuleCase1{
	// This used to be one large test class, which had separate table entries for each test, but put them all into the same table.  The result was
	// difficult if not impossible to debug, and the resulting detailed table was inconsistent in the sense that it did not contain all combinations of
	// entries. -- I have now pulled this apart into 6 different test classes, this one here plus "Case1" to "Case5".  Things look ok, but given that the
	// single class before was so large that I could not fully comprehend it, there may now be errors in the ripped-apart classes.  Hopefully, over time,
	// this will help to sort things out.  kai, feb'20

	//Old list of pollutants
//	private final Set<String> pollutants = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2,PM, SO2));
	private static final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ));
	private static final int leaveTime = 0;
	private final EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod;
	private static final String PASSENGER_CAR = "PASSENGER_CAR";

	private WarmEmissionAnalysisModule emissionsModule;

	// emission factors for tables - no duplicates!
	private static final Double DETAILED_PETROL_FACTOR_FF = .1;

	// vehicle information for regular test cases
	// case 1 - data in both tables -> use detailed
	private static final String PETROL_TECHNOLOGY = "PC petrol <1,4L";
	private static final String PETROL_SIZE_CLASS ="<ECE petrol (4S)";
	private static final String PETROL_CONCEPT ="<1,4L";
	private static final Double PETROL_SPEED_FF = 20.; //km/h
	private static final Double PETROL_SPEED_SG = 10.; //km/h

	@Parameterized.Parameters( name = "{index}: ComputationMethod={0}")
	public static Collection<Object[]> createCombinations() {
		List <Object[]> list = new ArrayList<>();
		list.add( new Object [] {EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction} ) ;
		list.add( new Object [] {EmissionsConfigGroup.EmissionsComputationMethod.AverageSpeed} ) ;
		return list;
	}

	public TestWarmEmissionAnalysisModuleCase1( EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod ) {
		this.emissionsComputationMethod = emissionsComputationMethod;
	}

	private void setUp() {

		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();

		TestWarmEmissionAnalysisModule.fillAverageTable( avgHbefaWarmTable );
		fillDetailedTable( detailedHbefaWarmTable );
		Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(
				avgHbefaWarmTable );
		TestWarmEmissionAnalysisModule.addDetailedRecordsToTestSpeedsTable( hbefaRoadTrafficSpeeds, detailedHbefaWarmTable );

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( this.emissionsComputationMethod );
		ecg.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );

		emissionsModule = new WarmEmissionAnalysisModule( avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, emissionEventManager, ecg );

	}



	@Test
	public void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent1(){
		//-- set up tables, event handler, parameters, module
		setUp();

		// case 1 - data in both tables -> use detailed
		Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);
		double linkLength = 200.;

		Link mockLink = createMockLink("link 1", linkLength, PETROL_SPEED_FF / 3.6 );

		Id<VehicleType> vehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( vehicle, mockLink, linkLength / PETROL_SPEED_FF * 3.6 );
		Assert.assertEquals( DETAILED_PETROL_FACTOR_FF *linkLength/1000., warmEmissions.get( CO2_TOTAL ), MatsimTestUtils.EPSILON );

		HandlerToTestEmissionAnalysisModules.reset();

		emissionsModule.throwWarmEmissionEvent(leaveTime, mockLink.getId(), vehicleId, warmEmissions );
		Assert.assertEquals( pollutants.size() * DETAILED_PETROL_FACTOR_FF *linkLength/1000., HandlerToTestEmissionAnalysisModules.getSum(), MatsimTestUtils.EPSILON );
		HandlerToTestEmissionAnalysisModules.reset();
		warmEmissions.clear();
	}

	@Test
	public void testCounters1(){
		setUp();
		emissionsModule.reset();

		/*
		 * using the same case as above - case 1 and check the counters for all possible combinations of avg, stop go and free flow speed
		 */

		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		String roadType = "0";
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		Link mockLink = createMockLink("link 1", linkLength, PETROL_SPEED_FF / 3.6 );


		// <stop&go speed
		Double travelTime = linkLength/ PETROL_SPEED_SG *1.2;
		Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( vehicle, mockLink, travelTime * 3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(linkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// = s&g speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength/ PETROL_SPEED_SG *3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(linkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// > s&g speed, <ff speed
		// speed in km/h
		travelTime = .5 * linkLength/ PETROL_SPEED_FF *3.6 + .5* (linkLength/ PETROL_SPEED_SG)*3.6; //540 seconds
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime );
		switch( emissionsComputationMethod ) {
			// the following may be different for the wrong reasons, see comments in the ...Event2 method above.  kai, jan'20
			case StopAndGoFraction:
				Assert.assertEquals(1., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
				Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
				Assert.assertEquals(1., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
				break;
			case AverageSpeed:
				Assert.assertEquals(2., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
				Assert.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
				Assert.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
		}
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		Assert.assertEquals( emissionsModule.getKmCounter(), (emissionsModule.getStopGoKmCounter()+ emissionsModule.getFreeFlowKmCounter()), MatsimTestUtils.EPSILON );
		//this assert is no longer relevant. More tests will be added to check the traffic situations, jm oct'18
		//Assert.assertEquals(travelTime, 3600*weam.getFreeFlowKmCounter()/petrolSpeedFf+3600*weam.getStopGoKmCounter()/petrolSpeedSg, MatsimTestUtils.EPSILON);
		emissionsModule.reset();

		// = ff speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength/ PETROL_SPEED_FF *3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		//> ff speed
		boolean exceptionThrown = false;
		try{
			warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, 0.4*linkLength/ PETROL_SPEED_FF *3.6 );
		}catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assert.assertTrue("An average speed higher than the free flow speed should throw a runtime exception", exceptionThrown);
		emissionsModule.reset();
	}

	@Test
	public void testCounters5(){
		setUp();
		emissionsModule.reset();

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
		Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( inconffVehicle, inconLink,
				inconff / PETROL_SPEED_FF * 3.6 );

		switch( emissionsComputationMethod ) {
			// the following may be different for the wrong reasons, see comments in the ...Event2 method above.  kai, jan'20
			case StopAndGoFraction:
				Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
				break;
			case AverageSpeed:
				Assert.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
		}

		Assert.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// average speed equals wrong free flow speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, inconff/inconffavgSpeed*3.6 );
		Assert.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );

		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, 2*inconff/(PETROL_SPEED_FF + PETROL_SPEED_SG)*3.6 );
	}



	@Test
	@Ignore
	public void testCounters1fractional(){
		setUp();
		emissionsModule.getEcg().setEmissionsComputationMethod(StopAndGoFraction );
		// yyyyyy !!!!!!

		emissionsModule.reset();

		/*
		 * using the same case as above - case 1 and check the counters for all possible combinations of avg, stop go and free flow speed
		 */

		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		String roadType = "0";
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create( PASSENGER_CAR + ";"+ PETROL_TECHNOLOGY +";"+ PETROL_SIZE_CLASS +";"+ PETROL_CONCEPT, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		Link mockLink = createMockLink("link 1", linkLength, PETROL_SPEED_FF / 3.6 );


		// <stop&go speed
		Double travelTime = linkLength/ PETROL_SPEED_SG *1.2;
		Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( vehicle, mockLink, travelTime * 3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(linkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// = s&g speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength/ PETROL_SPEED_SG *3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(linkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// > s&g speed, <ff speed
		// speed in km/h
		travelTime = .5 * linkLength/ PETROL_SPEED_FF *3.6 + .5* (linkLength/ PETROL_SPEED_SG)*3.6; //540 seconds
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, travelTime );
		Assert.assertEquals(1, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(1., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		Assert.assertEquals( emissionsModule.getKmCounter(), (emissionsModule.getStopGoKmCounter()+ emissionsModule.getFreeFlowKmCounter()), MatsimTestUtils.EPSILON );
		Assert.assertEquals(travelTime, 3600* emissionsModule.getFreeFlowKmCounter()/ PETROL_SPEED_FF +3600* emissionsModule.getStopGoKmCounter()/ PETROL_SPEED_SG, MatsimTestUtils.EPSILON );
		emissionsModule.reset();

		// = ff speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, linkLength/ PETROL_SPEED_FF *3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(linkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		//> ff speed
		boolean exceptionThrown = false;
		try{
			warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, mockLink, 0.4*linkLength/ PETROL_SPEED_FF *3.6 );
		}catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assert.assertTrue("An average speed higher than the free flow speed should throw a runtime exception", exceptionThrown);

		emissionsModule.reset();
		emissionsModule.getEcg().setEmissionsComputationMethod(AverageSpeed );
		// yyyyyy !!!!!!

	}

	@Test
	@Ignore
	public void testCounters6(){
		setUp();
		emissionsModule.getEcg().setEmissionsComputationMethod(StopAndGoFraction );
		emissionsModule.reset();

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
		Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( inconffVehicle, inconLink,
				inconff / PETROL_SPEED_FF * 3.6 );
		Assert.assertEquals(1, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// average speed equals wrong free flow speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, inconff/inconffavgSpeed*3.6 );
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(inconff/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );

		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(inconffVehicle, inconLink, 2*inconff/(PETROL_SPEED_FF + PETROL_SPEED_SG)*3.6 );
	}

	@Test
	public void testCounters8(){
		setUp();
		emissionsModule.reset();

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
		Assert.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assert.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), 1e-7 );
		Assert.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assert.assertEquals(0., emissionsModule.getKmCounter(), 1e-7 );
		Assert.assertEquals(0., emissionsModule.getStopGoKmCounter(), 1e-7 );
		Assert.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assert.assertEquals(0, emissionsModule.getWarmEmissionEventCounter() );

		// < s&g speed
		Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions( vehicle, pcLink, travelTime * 3.6 );
		// = s&g speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, linkLength/ PETROL_SPEED_SG *3.6 );
		// > s&g speed, <ff speed
		travelTime = .5 * linkLength/ PETROL_SPEED_FF *3.6 + .5* (linkLength/ PETROL_SPEED_SG)*3.6; //540 seconds
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime );
		// = ff speed
		warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, linkLength/ PETROL_SPEED_FF *3.6 );
		//> ff speed - has been tested to throw runtime exceptions
	}



	private static void fillDetailedTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable ) {

		// petrol free-flow:
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology( PETROL_TECHNOLOGY );
			vehAtt.setHbefaSizeClass( PETROL_SIZE_CLASS );
			vehAtt.setHbefaEmConcept( PETROL_CONCEPT );

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
			detWarmFactor.setWarmEmissionFactor( DETAILED_PETROL_FACTOR_FF );
			detWarmFactor.setSpeed( PETROL_SPEED_FF );

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setHbefaComponent( wp );
				detWarmKey.setHbefaRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setHbefaTrafficSituation( HbefaTrafficSituation.FREEFLOW );
				detWarmKey.setHbefaVehicleAttributes( vehAtt );
				detWarmKey.setHbefaVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactor );

			}
		}

		// petrol stop-go:
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology( PETROL_TECHNOLOGY );
			vehAtt.setHbefaSizeClass( PETROL_SIZE_CLASS );
			vehAtt.setHbefaEmConcept( PETROL_CONCEPT );

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor();
			double detailedPetrolFactorSg = .01;
			detWarmFactor.setWarmEmissionFactor( detailedPetrolFactorSg );
			detWarmFactor.setSpeed( PETROL_SPEED_SG );

			for( Pollutant wp : pollutants ){
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setHbefaComponent( wp );
				detWarmKey.setHbefaRoadCategory( HBEFA_ROAD_CATEGORY );
				detWarmKey.setHbefaTrafficSituation( HbefaTrafficSituation.STOPANDGO );
				detWarmKey.setHbefaVehicleAttributes( vehAtt );
				detWarmKey.setHbefaVehicleCategory( HbefaVehicleCategory.PASSENGER_CAR );
				detailedHbefaWarmTable.put( detWarmKey, detWarmFactor );
			}
		}

	}

}
	

	

