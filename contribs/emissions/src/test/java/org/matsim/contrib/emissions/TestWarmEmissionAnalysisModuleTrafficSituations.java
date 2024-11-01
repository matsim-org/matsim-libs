package org.matsim.contrib.emissions;/* *********************************************************************** *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.*;

import static org.matsim.contrib.emissions.HbefaTrafficSituation.*;
import static org.matsim.contrib.emissions.Pollutant.NOx;


/**
 * @author joe


/*
 * test for playground.vsp.emissions.WarmEmissionAnalysisModule
 *
 **/

public class TestWarmEmissionAnalysisModuleTrafficSituations {


	private static final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ) );
	private static final String hbefaRoadCategory = "URB";
	private final String passengercar= "PASSENGER_CAR";

	private WarmEmissionAnalysisModule weam;
	private Map<Pollutant, Double> warmEmissions;

	//average speeds should be the same across all car types, but vary by traffic situation
	private static final int FF_INDEX = 0;
	private static final int HEAVY_INDEX = 1;
	private static final int SAT_INDEX = 2;
	private static final int SG_INDEX = 3;
	private final HbefaTrafficSituation[] trafficSituations = {FREEFLOW, HEAVY, SATURATED, STOPANDGO, STOPANDGO_HEAVY};
	private final double[] avgPassengerCarSpeed = {51., 40., 30., 10., 5.};

	// vehicle information for regular test cases
	// case 1 - data in both tables -> use detailed
	private static final String petrolTechnology = "PC petrol <1,4L";
	private static final String petrolSizeClass ="<ECE petrol (4S)";
	private static final String petrolConcept ="<1,4L";
	private static final double[] detailedPetrolFactor = {10, 100, 1000, 10000, 100000};

	// case 2 - free flow entry in both tables, stop-go entry in average table -> use average
	private static final String pcTechnology = "PC petrol <1,4L <ECE";
	private static final String pcSizeClass = "petrol (4S)";
	private static final String pcConcept = "<1,4L";
	private static final double[] avgPetrolFactor = {20, 200, 2000, 20000, 200000};

	public void setUp(EmissionsComputationMethod emissionsComputationMethod) {
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
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort); //declare using detailed values

		weam = new WarmEmissionAnalysisModule( avgHbefaWarmTable, detailedHbefaWarmTable, hbefaRoadTrafficSpeeds, pollutants, emissionEventManager, ecg );


	}


	//Test to check that vehicles not found in the detailed table revert back to average table - ie detailed (petrol, 1,2,3), average (petrol), search pet 4
	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	public void testFallBackToAverageTable(EmissionsComputationMethod emissionsComputationMethod) {
		setUp(emissionsComputationMethod);
		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create(passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept, VehicleType.class);
		double ffspeed = avgPassengerCarSpeed[FF_INDEX];
		double travelTime = linkLength/ffspeed;
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));
		Link pcLink = createMockLink(linkLength, ffspeed / 3.6);

		//allow fallback to average table
		weam.getEcg().setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		Assertions.assertEquals(detailedPetrolFactor[FF_INDEX]*(linkLength/1000.), warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );

		vehicleTypeId = Id.create(passengercar+ ";"+pcTechnology+";"+pcSizeClass+";"+pcConcept, VehicleType.class);
		vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		Assertions.assertEquals(avgPetrolFactor[FF_INDEX]*(linkLength/1000.), warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );

	}

	//using the different computation methods, the NOx warm Emissions are calculated for the different traffic situations (FF;HEAVY;SAT;SG)
	@ParameterizedTest
	@EnumSource(EmissionsConfigGroup.EmissionsComputationMethod.class)
	public void testTrafficSituations(EmissionsComputationMethod emissionsComputationMethod) {
		setUp(emissionsComputationMethod);
		Id<Vehicle> vehicleId = Id.create("vehicle 1", Vehicle.class);
		double linkLength = 2*1000.; //in meter
		Id<VehicleType> vehicleTypeId = Id.create(passengercar+ ";"+petrolTechnology+";"+petrolSizeClass+";"+petrolConcept, VehicleType.class);
		VehiclesFactory vehFac = VehicleUtils.getFactory();
		Vehicle vehicle = vehFac.createVehicle(vehicleId, vehFac.createVehicleType(vehicleTypeId));

		double ffspeed = avgPassengerCarSpeed[FF_INDEX];

		Link pcLink = createMockLink(linkLength, ffspeed / 3.6);

		double actualSpeed = avgPassengerCarSpeed[FF_INDEX];
		double travelTime = linkLength/actualSpeed;
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		Assertions.assertEquals(detailedPetrolFactor[FF_INDEX]*(linkLength/1000.), warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );

		actualSpeed = avgPassengerCarSpeed[HEAVY_INDEX];
		travelTime = linkLength/actualSpeed;
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		switch( emissionsComputationMethod ) {
			case StopAndGoFraction:
				Assertions.assertEquals( 1360.1219512195123, warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );
				break;
			case AverageSpeed:
				Assertions.assertEquals(detailedPetrolFactor[HEAVY_INDEX]*(linkLength/1000.), warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
		}

		actualSpeed = avgPassengerCarSpeed[SAT_INDEX];
		travelTime = linkLength/actualSpeed;
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		switch( emissionsComputationMethod ) {
			case StopAndGoFraction:
				Assertions.assertEquals( 3431.219512195123, warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );
				break;
			case AverageSpeed:
				Assertions.assertEquals(detailedPetrolFactor[SAT_INDEX]*(linkLength/1000.), warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
		}

		actualSpeed = avgPassengerCarSpeed[SG_INDEX];
		travelTime = linkLength/actualSpeed;
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		Assertions.assertEquals(detailedPetrolFactor[SG_INDEX]*(linkLength/1000.), warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );

		actualSpeed = avgPassengerCarSpeed[SG_INDEX] + 5;
		travelTime = linkLength/actualSpeed;
		warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime*3.6);
		switch( emissionsComputationMethod ) {
			case StopAndGoFraction:
				Assertions.assertEquals(11715.609756097561, warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );
				break;

			case AverageSpeed:
				Assertions.assertEquals(detailedPetrolFactor[SAT_INDEX]*(linkLength/1000.), warmEmissions.get( NOx ), MatsimTestUtils.EPSILON );
				break;
			default:
				throw new IllegalStateException( "Unexpected value: " + emissionsComputationMethod );
		}


		{
			actualSpeed = avgPassengerCarSpeed[SG_INDEX] - 5;
			travelTime = linkLength / actualSpeed;
			warmEmissions = weam.checkVehicleInfoAndCalculateWarmEmissions(vehicle, pcLink, travelTime * 3.6);
			Assertions.assertEquals(detailedPetrolFactor[SG_INDEX] * (linkLength / 1000.), warmEmissions.get(NOx), MatsimTestUtils.EPSILON);

			//results should be equal here, because in both cases only the s&g value is relevant (0% freeflow, 100% stop&go).
			Assertions.assertEquals(20000, warmEmissions.get(NOx), MatsimTestUtils.EPSILON);

		}
	}



	private static Link createMockLink(double linkLength, double ffspeed) {
		Node mockNode1 = NetworkUtils.createNode(Id.createNodeId(1));
		Node mockNode2 = NetworkUtils.createNode(Id.createNodeId(2));
		Link l = NetworkUtils.createLink(Id.createLinkId("link"), mockNode1, mockNode2, null, linkLength, ffspeed, 1800, 1);
		EmissionUtils.setHbefaRoadType(l, "URB");
		return l;
	}

	private void fillDetailedTable( Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable) {

		for (int i = 0; i < trafficSituations.length; i++) {
			HbefaTrafficSituation trafficSituation = trafficSituations[i];
			double speed = avgPassengerCarSpeed[i];
			double factor = detailedPetrolFactor[i];

			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
			vehAtt.setHbefaTechnology(petrolTechnology);
			vehAtt.setHbefaSizeClass(petrolSizeClass);
			vehAtt.setHbefaEmConcept(petrolConcept);

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(factor, speed);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(hbefaRoadCategory);
				detWarmKey.setTrafficSituation(trafficSituation);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				detailedHbefaWarmTable.put(detWarmKey, detWarmFactor);

			}
		}

	}

	private void fillAverageTable(	Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable) {

		for (int i = 0; i < trafficSituations.length; i++) {
			HbefaTrafficSituation trafficSituation = trafficSituations[i];
			double speed = avgPassengerCarSpeed[i];
			double factor = avgPetrolFactor[i];

			HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();

			HbefaWarmEmissionFactor detWarmFactor = new HbefaWarmEmissionFactor(factor, speed);

			for (Pollutant wp : pollutants) {
				HbefaWarmEmissionFactorKey detWarmKey = new HbefaWarmEmissionFactorKey();
				detWarmKey.setComponent(wp);
				detWarmKey.setRoadCategory(hbefaRoadCategory);
				detWarmKey.setTrafficSituation(trafficSituation);
				detWarmKey.setVehicleAttributes(vehAtt);
				detWarmKey.setVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
				avgHbefaWarmTable.put(detWarmKey, detWarmFactor);

			}
		}

	}

	private void addDetailedRecordsToTestSpeedsTable(
			Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds,
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable
	) {
		detailedHbefaWarmTable.forEach((warmEmissionFactorKey, emissionFactor) -> {
			HbefaRoadVehicleCategoryKey roadVehicleCategoryKey = new HbefaRoadVehicleCategoryKey(warmEmissionFactorKey);
			HbefaTrafficSituation hbefaTrafficSituation = warmEmissionFactorKey.getTrafficSituation();
			double speed = emissionFactor.getSpeed();

			hbefaRoadTrafficSpeeds.putIfAbsent(roadVehicleCategoryKey, new HashMap<>());
			hbefaRoadTrafficSpeeds.get(roadVehicleCategoryKey).put(hbefaTrafficSituation, speed);
		});

	}


}




