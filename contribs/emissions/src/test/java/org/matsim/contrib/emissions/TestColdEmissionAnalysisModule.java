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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

import static org.matsim.contrib.emissions.HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
import static org.matsim.contrib.emissions.HbefaVehicleCategory.PASSENGER_CAR;
import static org.matsim.contrib.emissions.Pollutant.*;


/**
 * @author julia
 * <p>
 * test for playground.vsp.emissions.ColdEmissionAnalysisModule
 * <p>
 * ColdEmissionAnalysisModule (ceam)
 * public methods and corresponding tests:
 * ceam module parameter - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * ceam - constructor, nothing to test
 * reset - nothing to test
 * calculate cold emissions and throw event - calculateColdEmissionsAndThrowEventTest
 * <p>
 * private methods and corresponding tests:
 * rescale cold emissions - rescaleColdEmissionsTest
 * calculate cold emissions - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * convert string to tuple - implicitly tested in calculateColdEmissionAndThrowEventTest, rescaleColdEmissionTest
 * <p>
 * in all cases the needed tables are created manually by the setUp() method
 * see test methods for details on the particular test cases
 */

public class TestColdEmissionAnalysisModule {

	private final Double startTime = 0.0;
	private static final Double parkingDuration = 1.;
	// same values as int for table
	private static final int tableParkingDuration = (int) Math.round( parkingDuration );
	private static final int tableAccDistance = 1;
	private static final Set<Pollutant> pollutants = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2,PM, SO2));
	private final int numberOfColdEmissions = pollutants.size();
	// strings for test cases

	// The material below was confused in the way that strings like "petrol" or "diesel" were given for the
	// size classes, and "<1,4L" or ">=2L" for the emissions concept.  Tried to make it consistent,
	// but I don't know if it is still testing the original functionality.  kai, jul'18

	// first case: complete data - corresponding entry in average table
	private static final String petrol_technology = "petrol";
	private static final String none_sizeClass = "average";
	private static final String none_emConcept = "average";
	// second case: complete data - corresponding entry in detailed table
	private static final String petrol_technology2 = "petrol";  // maybe use same as above? kai, jul'18
	private static final String leq14l_sizeClass = "<1,4L";
	private static final String PC_P_Euro_1_emConcept = "PC-P-Euro-1";
	// third case: complete data corresponding entries in average and detailed table - use detailed
	private static final String diesel_technology = "diesel";
	private static final String geq2l_sizeClass = ">=2L";
	private static final String PC_D_Euro_3_emConcept = "PC-D-Euro-3";

	// emission factors for tables - no dublicates!
	private static final Double detailedPetrolFactor = 100.;
	private static final Double detailedDieselFactor = 10.;
	private static final Double averageAverageFactor = .1;
	private static final Double averagePetrolFactor = .01;

	private static final double fakeFactor = -1.;

	private boolean excep = false;


	/*
	 * four test cases with missing information
	 * all of them should throw exceptions
	 */
	@Test
	void calculateColdEmissionsAndThrowEventTest_Exceptions() {

		ColdEmissionAnalysisModule coldEmissionAnalysisModule  = setUp();
		List<Id<VehicleType>> testCasesExceptions = new ArrayList<>();
		excep = false;

		//case: vehicle category not specified
		testCasesExceptions.add( Id.create( ";;;", VehicleType.class ) );
		//case: empty string as id
		testCasesExceptions.add( Id.create( "", VehicleType.class ) );
		//case: null id
		testCasesExceptions.add( null );

		for ( Id<VehicleType> vehicleTypeId : testCasesExceptions ) {
			String message = "'" + vehicleTypeId + "'" + " was used to calculate cold emissions and generate an emissions event."
					+ "It should instead throw an exception because it is not a valid vehicle information string.";
			try {
				Id<Link> linkId = Id.create("linkId" + testCasesExceptions.indexOf(vehicleTypeId), Link.class);
				Id<Vehicle> vehicleId = Id.create("vehicleId" + testCasesExceptions.indexOf(vehicleTypeId), Vehicle.class);
				Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, VehicleUtils.getFactory().createVehicleType(vehicleTypeId));
				coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicle.getId(), linkId, startTime, parkingDuration, tableAccDistance);
			} catch (Exception e) {
				excep = true;
			}
			Assertions.assertTrue(excep, message);
			excep = false;
		}

	}

	@Test
	void calculateColdEmissionsAndThrowEventTest_minimalVehicleInformation() {

		ColdEmissionAnalysisModule coldEmissionAnalysisModule  = setUp();
		excep = false;

		// case: no specifications for technology, size, class, em concept
		// string has no semicolons as separators - use average values
		Id<VehicleType> vehInfo11 = Id.create("PASSENGER_CAR", VehicleType.class );
		Id<Link> linkId11 = Id.create( "link id 11", Link.class );
		Id<Vehicle> vehicleId7 = Id.create( "vehicle 11", Vehicle.class );

		Vehicle vehicle = VehicleUtils.getFactory().createVehicle( vehicleId7, VehicleUtils.getFactory().createVehicleType( vehInfo11 ) );

		Map<Pollutant, Double> calculatedPollutants = coldEmissionAnalysisModule.checkVehicleInfoAndCalculateWColdEmissions(vehicle.getType(), vehicle.getId(), linkId11, startTime, parkingDuration, tableAccDistance);

		double sumOfEmissions = calculatedPollutants.values().stream().mapToDouble(Double::doubleValue).sum();

		String message = "The expected emissions for an emissions event with vehicle information string '" + vehInfo11 + "' are " +
						     numberOfColdEmissions * averageAverageFactor + " but were " + sumOfEmissions;
		Assertions.assertEquals( numberOfColdEmissions * averageAverageFactor, sumOfEmissions, MatsimTestUtils.EPSILON, message );

	}

	private static ColdEmissionAnalysisModule setUp() {
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable = new HashMap<>();
		Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable = new HashMap<>();

		fillAveragesTable( avgHbefaColdTable );
		fillDetailedTable( detailedHbefaColdTable );

		EventsManager emissionEventManager = new HandlerToTestEmissionAnalysisModules();
		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.usingVehicleTypeId );

		//This represents the previous behavior, which fallbacks to the average table, if values are not found in the detailed table, kmt apr'20
		ecg.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
		return new ColdEmissionAnalysisModule( avgHbefaColdTable, detailedHbefaColdTable, ecg, pollutants, emissionEventManager );

	}


	private static void fillDetailedTable( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable ) {
		// create all needed and one unneeded entry for the detailed table
		{
			// add passenger car entry "petrol;<=1.4L;PC-P-Euro-1":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology2, leq14l_sizeClass, PC_P_Euro_1_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( detailedPetrolFactor ), PASSENGER_CAR );
		}
		{
			// add passenger car entry "diesel;>=2L;PC-D-Euro-3":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( diesel_technology, geq2l_sizeClass, PC_D_Euro_3_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( detailedDieselFactor ), PASSENGER_CAR );
		}
		{
			// add heavy goods vehicle entry "petrol;none;none":
			//(pre-existing comment: HEAVY_GOODS_VEHICLE;PC petrol;petrol;none should not be used --???)
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );
			putIntoHbefaColdTable( detailedHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( fakeFactor ), HEAVY_GOODS_VEHICLE );
		}

	}

	private static void fillAveragesTable( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable ) {
		// create all needed and one unneeded entry for the average table
		{
			// add passenger car entry "average;average;average":
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( "average", "average", "average" ) ;

			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor(averageAverageFactor), PASSENGER_CAR );
		}
		{
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );

			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( averagePetrolFactor ), PASSENGER_CAR );
		}
		{
			// duplicate from detailed table, but with different emission factor.
			// this should not be used but is needed to assure that the detailed table is tried before the average table
			HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( diesel_technology, geq2l_sizeClass, PC_D_Euro_3_emConcept );

			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( fakeFactor ), PASSENGER_CAR );
		}
		{
			// add HGV entry "petrol;none;none".
			// (pre-existing comment: HEAVY_GOODS_VEHICLE;PC petrol;petrol;none should not be used --???)
			final HbefaVehicleAttributes vehAtt = ColdEmissionAnalysisModule.createHbefaVehicleAttributes( petrol_technology, none_sizeClass, none_emConcept );

			putIntoHbefaColdTable( avgHbefaColdTable, vehAtt, new HbefaColdEmissionFactor( fakeFactor ), HEAVY_GOODS_VEHICLE );
		}
	}

	private static void putIntoHbefaColdTable( final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable, final HbefaVehicleAttributes vehAtt, final HbefaColdEmissionFactor detColdFactor, final HbefaVehicleCategory hbefaVehicleCategory ) {
		for ( Pollutant cp : pollutants ) {
			HbefaColdEmissionFactorKey detColdKey = new HbefaColdEmissionFactorKey();
			detColdKey.setDistance(tableAccDistance);
			detColdKey.setParkingTime(tableParkingDuration);
			detColdKey.setVehicleAttributes(vehAtt);
			detColdKey.setVehicleCategory(hbefaVehicleCategory);
			detColdKey.setComponent(cp);
			detailedHbefaColdTable.put(detColdKey, detColdFactor);
		}
	}

}
