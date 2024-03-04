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

import org.geotools.metadata.iso.quality.TemporalAccuracyImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;
import java.util.stream.Stream;

import static org.matsim.contrib.emissions.Pollutant.*;

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
	// This used to be one large test class, which had separate table entries for each test, but put them all into the same table.  The result was
	// difficult if not impossible to debug, and the resulting detailed table was inconsistent in the sense that it did not contain all combinations of
	// entries. -- I have now pulled this apart into 6 different test classes, this one here plus "Case1" to "Case5".  Things look ok, but given that the
	// single class before was so large that I could not fully comprehend it, there may now be errors in the ripped-apart classes.  Hopefully, over time,
	// this will help to sort things out.  kai, feb'20

	private static final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ));
	static final String HBEFA_ROAD_CATEGORY = "URB";
	private boolean excep = false;
	private static final String PASSENGER_CAR = "PASSENGER_CAR";

	private WarmEmissionAnalysisModule emissionsModule;

	//average speeds should be the same across all car types, but vary by traffic situation
	static final Double AVG_PASSENGER_CAR_SPEED_FF_KMH = 20.;
	static final Double AVG_PASSENGER_CAR_SPEED_SG_KMH = 10.;
	// (These must be in kmh, since at some later point they are divided by 3.6.  However, this makes them awfully slow for practical purposes ...  kai, jan'20)

	// emission factors for tables - no duplicates!
	private static final Double DETAILED_SGFF_FACTOR_FF =   .000011;
	private static final Double DETAILED_SGFF_FACTOR_SG = 	.0000011;
	private static final Double AVG_PC_FACTOR_FF = 1.;
	private static final Double AVG_PC_FACTOR_SG = 10.;

	// vehicle information for regular test cases

	private static final Double PETROL_SPEED_FF = TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_FF_KMH;
	private static final Double PETROL_SPEED_SG = TestWarmEmissionAnalysisModule.AVG_PASSENGER_CAR_SPEED_SG_KMH;

	private final Double noeFreeSpeed = AVG_PASSENGER_CAR_SPEED_FF_KMH;

	// case 6 - data in detailed table, stop go speed = free flow speed
	private final String sgffRoadCatgory = "URB_case7";
	private final String sgffTechnology = "sg ff technology";
	private final String sgffConcept = "sg ff concept";
	private final String sgffSizeClass = "sg ff size class";

	public static Stream<EmissionsConfigGroup.EmissionsComputationMethod> arguments() {
		return Stream.of(
			EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction,
			EmissionsConfigGroup.EmissionsComputationMethod.AverageSpeed
		);
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		//-- set up tables, event handler, parameters, module
		setUp(emissionsComputationMethod);

		Id<Vehicle> sgffVehicleId = Id.create("vehicle sg equals ff", Vehicle.class);
		double sgffLinklength = 4000.;
		Link sgflink = createMockLink("link sgf", sgffLinklength, AVG_PASSENGER_CAR_SPEED_FF_KMH / 3.6);
		EmissionUtils.setHbefaRoadType(sgflink, sgffRoadCatgory);

		Id<VehicleType> sgffVehicleTypeId = Id.create( PASSENGER_CAR + ";" + sgffTechnology + ";"+ sgffSizeClass + ";"+sgffConcept, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle sgffVehicle = vehFac.createVehicle(sgffVehicleId, vehFac.createVehicleType(sgffVehicleTypeId));

		//avg>ff
		boolean exceptionThrown = false;
		try{
			emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(sgffVehicle, sgflink, .5 * sgffLinklength / AVG_PASSENGER_CAR_SPEED_FF_KMH * 3.6);
		}
		catch(RuntimeException re){
			exceptionThrown = true;
		}
		Assertions.assertTrue(exceptionThrown,"An average speed higher than the free flow speed should throw a runtime exception");


		{ //avg=ff=sg -> use ff factors
			Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(sgffVehicle, sgflink, sgffLinklength / AVG_PASSENGER_CAR_SPEED_FF_KMH * 3.6);
			Assertions.assertEquals(DETAILED_SGFF_FACTOR_FF * sgffLinklength / 1000., warmEmissions.get(NO2), MatsimTestUtils.EPSILON);
		}


		{ //avg<sg -> use sg factors
			Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(sgffVehicle, sgflink, 2*sgffLinklength/ AVG_PASSENGER_CAR_SPEED_FF_KMH *3.6 );
			Assertions.assertEquals( DETAILED_SGFF_FACTOR_SG *sgffLinklength/1000., warmEmissions.get(NO2 ), MatsimTestUtils.EPSILON );
		}
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions1(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		//-- set up tables, event handler, parameters, module
		setUp(emissionsComputationMethod);

		// case 5 - no entry in any table - must be different to other test case's strings
		//With the bug fix to handle missing values - this test should no longer throw an error - jm oct'18

		Id<Vehicle> noeVehicleId = Id.create("veh 5", Vehicle.class);
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 5", noeLinkLength, noeFreeSpeed);

		String noeSizeClass = "size";
		String noeConcept = "concept";
		String noeTechnology = "technology";
		Id<VehicleType> noeVehicleTypeId = Id.create( PASSENGER_CAR + ";"+ noeTechnology + ";" + noeSizeClass + ";" + noeConcept, VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(noeVehicleTypeId));

		excep= false;
		try{
			Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions
											(noeVehicle, noelink, 1.5* noeLinkLength /noeFreeSpeed);
			emissionsModule.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions );
		}catch(Exception e){
			excep = true;
		}
		Assertions.assertFalse(excep);

		excep=false;
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions2(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		//-- set up tables, event handler, parameters, module
		setUp(emissionsComputationMethod);

		// no vehicle information given
		Id<Vehicle> noeVehicleId = Id.create("veh 6", Vehicle.class);
		Id<VehicleType> noeVehicleTypeId = Id.create("", VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 6", noeLinkLength, noeFreeSpeed);

		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(noeVehicleTypeId));

		excep= false;
		try{
			Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions
											(noeVehicle, noelink, 1.5* noeLinkLength /noeFreeSpeed);
			emissionsModule.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions );
		}catch(Exception e){
			excep = true;
		}
		Assertions.assertTrue(excep); excep=false;
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions3(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		//-- set up tables, event handler, parameters, module
		setUp(emissionsComputationMethod);

		// empty vehicle information
		Id<Vehicle> noeVehicleId = Id.create("veh 7", Vehicle.class);
		Id<VehicleType> noeVehicleTypeId = Id.create(";;;", VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(noeVehicleTypeId));
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 7", noeLinkLength, noeFreeSpeed);

		excep= false;
		try{
			Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions
											(noeVehicle, noelink, 1.5*noeLinkLength/noeFreeSpeed);
			emissionsModule.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions );
		}catch(Exception e){
			excep = true;
		}
		Assertions.assertTrue(excep); excep=false;
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent_Exceptions4(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		//-- set up tables, event handler, parameters, module
		setUp(emissionsComputationMethod);
		//  vehicle information string is 'null'
		Id<Vehicle> noeVehicleId = Id.create("veh 8", Vehicle.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle noeVehicle = vehFac.createVehicle(noeVehicleId, vehFac.createVehicleType(null));
		double noeLinkLength = 22.;
		Link noelink = createMockLink("link 8", noeLinkLength, noeFreeSpeed);

		excep= false;
		try{
			Map<Pollutant, Double> warmEmissions = emissionsModule.checkVehicleInfoAndCalculateWarmEmissions
											(noeVehicle, noelink, 1.5*22./noeFreeSpeed);
			emissionsModule.throwWarmEmissionEvent(10., noelink.getId(), noeVehicleId, warmEmissions );
		}catch(Exception e){
			excep = true;
		}
		Assertions.assertTrue(excep); excep=false;

	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testCounters7(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod){
		setUp(emissionsComputationMethod);
		emissionsModule.reset();

		// case 10 - data in detailed table, stop go speed > free flow speed
		Id<Vehicle> tableVehicleId = Id.create("vehicle 8", Vehicle.class);
		double tableLinkLength= 30.*1000;
		Id<VehicleType> tableVehicleTypeId = Id.create(
				PASSENGER_CAR + ";" + "petrol (4S)" +";" + "<1,4L" +";"+ "PC-P-Euro-0", VehicleType.class );
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle tableVehicle = vehFac.createVehicle(tableVehicleId, vehFac.createVehicleType(tableVehicleTypeId));
		Link tableLink = createMockLink("link table", tableLinkLength, AVG_PASSENGER_CAR_SPEED_FF_KMH / 3.6);

		// ff < avg < ff+1 - handled like free flow
		double travelTime =  tableLinkLength/(AVG_PASSENGER_CAR_SPEED_FF_KMH +0.5)*3.6;
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(tableVehicle, tableLink, travelTime );
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(tableLinkLength/1000., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(tableLinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0., emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();

		// ff < sg < avg - handled like free flow as well - no additional test needed
		// avg < ff < sg - handled like stop go
		emissionsModule.checkVehicleInfoAndCalculateWarmEmissions(tableVehicle, tableLink, 2* tableLinkLength/(AVG_PASSENGER_CAR_SPEED_FF_KMH)*3.6 );
		Assertions.assertEquals(0, emissionsModule.getFractionOccurences() );
		Assertions.assertEquals(0., emissionsModule.getFreeFlowKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(0, emissionsModule.getFreeFlowOccurences() );
		Assertions.assertEquals(tableLinkLength/1000, emissionsModule.getKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(tableLinkLength/1000, emissionsModule.getStopGoKmCounter(), MatsimTestUtils.EPSILON );
		Assertions.assertEquals(1, emissionsModule.getStopGoOccurences() );
		Assertions.assertEquals(1, emissionsModule.getWarmEmissionEventCounter() );
		emissionsModule.reset();
	}


	private void setUp(EmissionsConfigGroup.EmissionsComputationMethod emissionsComputationMethod) {

		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable = new HashMap<>();
		Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable = new HashMap<>();

		fillAverageTable( avgHbefaWarmTable );
		fillDetailedTable( detailedHbefaWarmTable );
		Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds = EmissionUtils.createHBEFASpeedsTable(
				avgHbefaWarmTable );
		addDetailedRecordsToTestSpeedsTable( hbefaRoadTrafficSpeeds, detailedHbefaWarmTable );

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );
		ecg.setEmissionsComputationMethod( emissionsComputationMethod );
		ecg.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );

		emissionsModule = new WarmEmissionAnalysisModule( avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, emissionEventManager, ecg );

	}

	static Link createMockLink( String linkId, double linkLength, double ffspeed ) {
		Id<Link> mockLinkId = Id.createLinkId(linkId);
		Node mockNode1 = NetworkUtils.createNode(Id.createNodeId(1));
		Node mockNode2 = NetworkUtils.createNode(Id.createNodeId(2));
		Link l = NetworkUtils.createLink(mockLinkId, mockNode1, mockNode2, null, linkLength, ffspeed, 1800, 1);
		EmissionUtils.setHbefaRoadType(l, "URB");
		return l;
	}

	private void fillDetailedTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {

// entries for case sg speed = ff speed
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaEmConcept(sgffConcept);
			vehAtt.setHbefaSizeClass(sgffSizeClass);
			vehAtt.setHbefaTechnology(sgffTechnology);

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(DETAILED_SGFF_FACTOR_FF, AVG_PASSENGER_CAR_SPEED_FF_KMH);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(sgffRoadCatgory);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.FREEFLOW);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}
		{
			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaEmConcept(sgffConcept);
			vehAtt.setHbefaSizeClass(sgffSizeClass);
			vehAtt.setHbefaTechnology(sgffTechnology);

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(DETAILED_SGFF_FACTOR_SG, AVG_PASSENGER_CAR_SPEED_SG_KMH);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(sgffRoadCatgory);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.STOPANDGO);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}
	}

	static void fillAverageTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable ) {

		// entries for first case "petrol" should not be used since there are entries in the detailed table
		// there should only average vehicle attributes in the avgHebfWarmTable jm oct'18
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();


		// free flow:
		{
			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(AVG_PC_FACTOR_FF, PETROL_SPEED_FF);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.FREEFLOW);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}

		// stop and go:
		{
			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(AVG_PC_FACTOR_SG, PETROL_SPEED_SG);
			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(HBEFA_ROAD_CATEGORY);
				detWarmKey.setTrafficSituation(HbefaTrafficSituation.STOPANDGO);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				avgHbefaWarmTable.put(detWarmKey, detWarmFactor);
			}
		}
	}

	static void addDetailedRecordsToTestSpeedsTable(
			Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds,
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable
						       ) {

		// go through all entries in detailed warm table:
		for (Map.Entry<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> entry : detailedHbefaWarmTable.entrySet()) {

			HbefaWarmEmissionFactorKey warmEmissionFactorKey = entry.getKey();
			HbefaWarmEmissionFactor emissionFactor = entry.getValue();

			// extract road vehicle category key (something like "petrol"-"URB/50")
			HbefaRoadVehicleCategoryKey roadVehicleCategoryKey = new HbefaRoadVehicleCategoryKey(warmEmissionFactorKey);

			// for that road-vehicle category key, add traffic situation and speed value from current emissions factor:
			hbefaRoadTrafficSpeeds.putIfAbsent(roadVehicleCategoryKey, new HashMap<>());


			// if the value is already set (returning not null) an exception is thrown
			// avoid overriding of speeds from average table with speed from detailed table
			if (hbefaRoadTrafficSpeeds.get(roadVehicleCategoryKey).get(warmEmissionFactorKey.getTrafficSituation()) != null) {
				if (hbefaRoadTrafficSpeeds.get(roadVehicleCategoryKey).get(warmEmissionFactorKey.getTrafficSituation()) != emissionFactor.getSpeed()) {

					throw new RuntimeException("Try to override speeds from average table with speed from detailed table. This may lead to wrong emission calculations. KMT/GR Aug'20");
				}
			}


			hbefaRoadTrafficSpeeds.get(roadVehicleCategoryKey).put(warmEmissionFactorKey.getTrafficSituation(), emissionFactor.getSpeed());
		}

	}


}




