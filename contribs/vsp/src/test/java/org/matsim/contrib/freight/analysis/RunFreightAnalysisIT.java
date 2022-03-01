/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.analysis;

import javafx.collections.ArrayChangeListener;
import org.junit.*;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.*;

import java.io.*;
import java.util.*;

public class RunFreightAnalysisIT {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Before
	public void runAnalysis(){
		final String packageInputDirectory = testUtils.getClassInputDirectory();
		final String outputDirectory = testUtils.getOutputDirectory();
		RunFreightAnalysis freightAnalysis = new RunFreightAnalysis(packageInputDirectory, outputDirectory);
		freightAnalysis.runAnalysis();
	}

	@Test
	public void compareResults() {
		//some generale stats
		checkFile("carrierStats.tsv");
		checkFile("freightVehicleStats.tsv");
		checkFile("freightVehicleTripStats.tsv");
		checkFile("serviceStats.tsv");
		checkFile("shipmentStats.tsv");

		//Carrier specific stats
		checkFile("carrier_carrier1_ServiceStats.tsv");
		checkFile("carrier_carrier1_ShipmentStats.tsv");
		checkFile("carrier_carrier1_VehicleTypeStats.tsv");
		checkFile("carrier_##carrier1_tripStats.tsv");  //Note: The "?" is here, because the carrierId was guessed depending on the vehicleId.
		checkFile("carrier_##carrier1_vehicleStats.tsv"); //Note: The "?" is here, because the carrierId was guessed depending on the vehicleId.
	}

	private void checkFile(String filename) {
		final String inputFilename = testUtils.getInputDirectory() + filename;
		final String outputFilename = testUtils.getOutputDirectory() + filename;
		MatsimTestUtils.compareFilesLineByLine(inputFilename, outputFilename);
	}

	@Test
	public void runVehicleTrackerTest(){
		final String inputPath = testUtils.getClassInputDirectory();
		File networkFile = new File(inputPath + "/output_network.xml.gz");
		File carrierFile = new File(inputPath + "/output_carriers.xml");
		File vehiclesFile = new File(inputPath + "/output_allVehicles.xml.gz");
		File eventsFile = new File(inputPath + "/output_events.xml.gz");

		Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		new  MatsimVehicleReader(vehicles).readFile(vehiclesFile.getAbsolutePath());

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		for( VehicleType vehicleType : vehicles.getVehicleTypes().values() ){
			carrierVehicleTypes.getVehicleTypes().put( vehicleType.getId(), vehicleType );
		}
		// yyyy the above is somewhat awkward.  ???

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes).readFile(carrierFile.getAbsolutePath());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		MyFreightVehicleTrackerEventHandler eventHandler = new MyFreightVehicleTrackerEventHandler(vehicles, network, carriers);
		eventsManager.addHandler(eventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);

		eventsReader.readFile(eventsFile.getAbsolutePath());
		eventsManager.finishProcessing();
		LinkedHashMap<Id<Vehicle>, VehicleTracker> tracker = eventHandler.getVehicleTracking().getTrackers();
		Iterator<Id<Vehicle>> keyItr = tracker.keySet().iterator();
		Id<Vehicle> lightVehicle_0 = Id.create( "freight_carrier1_veh_carrier_19_lightVehicle_0", Vehicle.class );
		Id<Vehicle> lightVehicle_1 = Id.create( "freight_carrier1_veh_carrier_19_lightVehicle_1", Vehicle.class );
		while(keyItr.hasNext()){
			Id<Vehicle> key = keyItr.next();
			System.out.println("road time "+tracker.get(key).roadTime);
			if(key.equals(lightVehicle_0)){
				Id<VehicleType> vehicleType = Id.create( "light", VehicleType.class );
				Id<Person> driverId_0 = Id.create( "freight_carrier1_veh_carrier_19_lightVehicle_0", Person.class );
				Assert.assertEquals("Road time is not as expected ",Double.valueOf(6195.0),tracker.get(key).roadTime);
				Assert.assertEquals("Cost is not as expected ",Double.valueOf(50.88999999999996),tracker.get(key).cost);
				Assert.assertEquals("Travel distance is not as expected ",Double.valueOf(35000.0),tracker.get(key).travelDistance);
				Assert.assertEquals("Usage time is not as expected ",Double.valueOf(6689.0),tracker.get(key).usageTime);
				Assert.assertEquals("Driver Id is not as expected ",driverId_0,tracker.get(key).lastDriverId);
				Assert.assertEquals("Vehicle type is not as expected ",vehicleType,tracker.get(key).vehicleType.getId());
				Assert.assertEquals("No: of trips is not as expected ",6,tracker.get(key).tripHistory.size());

				//trip history
				LinkedHashSet<VehicleTracker.VehicleTrip> tripHistory = tracker.get(key).tripHistory;
				Iterator<VehicleTracker.VehicleTrip> tripHistoryItr = tripHistory.stream().iterator();
				HashMap<Integer, VehicleTracker.VehicleTrip> history = new HashMap<>();
				int i = 0;
				while(tripHistoryItr.hasNext()){
					history.put(i, tripHistoryItr.next());
					i++;
				}
				Assert.assertEquals("cost is not as expected ",Double.valueOf(-6.0200000000000005),history.get(0).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(10000.0),history.get(0).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-1340.0),history.get(0).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_0,history.get(0).driverId);

				Assert.assertEquals("cost is not as expected ",Double.valueOf(-5.417999999999999),history.get(1).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(5000.0),history.get(1).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-971.0),history.get(1).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_0,history.get(1).driverId);

				Assert.assertEquals("cost is not as expected ",Double.valueOf(-4.214),history.get(2).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(3000.0),history.get(2).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-703.0),history.get(2).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_0,history.get(2).driverId);

				Assert.assertEquals("cost is not as expected ",Double.valueOf(-4.816),history.get(3).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(4000.0),history.get(3).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-837.0),history.get(3).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_0,history.get(3).driverId);

				Assert.assertEquals("cost is not as expected ",Double.valueOf(-4.816),history.get(4).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(4000.0),history.get(4).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-837.0),history.get(4).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_0,history.get(4).driverId);

				Assert.assertEquals("cost is not as expected ",Double.valueOf(-7.826000000000001),history.get(5).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(9000.0),history.get(5).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-1507.0),history.get(5).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_0,history.get(5).driverId);
				
			} else if(key.equals(lightVehicle_1)){
				Id<VehicleType> vehicleType = Id.create( "light", VehicleType.class );
				Id<Person> driverId_1 = Id.create( "freight_carrier1_veh_carrier_19_lightVehicle_1", Person.class );
				Assert.assertEquals("Road time is not as expected ",Double.valueOf(3684.0),tracker.get(key).roadTime);
				Assert.assertEquals("Cost is not as expected ",Double.valueOf(65.33799999999991),tracker.get(key).cost);
				Assert.assertEquals("Travel distance is not as expected ",Double.valueOf(23000.0),tracker.get(key).travelDistance);
				Assert.assertEquals("Usage time is not as expected ",Double.valueOf(3818.0),tracker.get(key).usageTime);
				Assert.assertEquals("Driver Id is not as expected ",driverId_1,tracker.get(key).lastDriverId);
				Assert.assertEquals("Vehicle type is not as expected ",vehicleType,tracker.get(key).vehicleType.getId());
				Assert.assertEquals("No: of trips is not as expected ",3,tracker.get(key).tripHistory.size());

				//trip history
				LinkedHashSet<VehicleTracker.VehicleTrip> tripHistory = tracker.get(key).tripHistory;
				Iterator<VehicleTracker.VehicleTrip> tripHistoryItr = tripHistory.stream().iterator();
				HashMap<Integer, VehicleTracker.VehicleTrip> history = new HashMap<>();
				int i = 0;
				while(tripHistoryItr.hasNext()){
					history.put(i, tripHistoryItr.next());
					i++;
				}
				Assert.assertEquals("cost is not as expected ",Double.valueOf(-3.6120000000000005),history.get(0).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(6000.0),history.get(0).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-804.0),history.get(0).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_1,history.get(0).driverId);

				Assert.assertEquals("cost is not as expected ",Double.valueOf(-6.622000000000001),history.get(1).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(7000.0),history.get(1).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-1239.0),history.get(1).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_1,history.get(1).driverId);

				Assert.assertEquals("cost is not as expected ",Double.valueOf(-8.428),history.get(2).cost);
				Assert.assertEquals("travelDistance is not as expected ",Double.valueOf(10000.0),history.get(2).travelDistance);
				Assert.assertEquals("travelTime is not as expected ",Double.valueOf(-1641.0),history.get(2).travelTime);
				Assert.assertEquals("driverId is not as expected ",driverId_1,history.get(2).driverId);

			}
		}

		//assertEquals(1200, eventHandler.getVehicleTracking().getTrackers().get("vehId").currentTripDistance);
		//...

	}

	@Test
	public void runServiceTrackerTest(){

		final String inputPath = testUtils.getClassInputDirectory();
		File networkFile = new File(inputPath + "/output_network.xml.gz");
		File carrierFile = new File(inputPath + "/output_carriers.xml");
		File vehiclesFile = new File(inputPath + "/output_allVehicles.xml.gz");
		File eventsFile = new File(inputPath + "/output_events.xml.gz");

		Network network = NetworkUtils.readNetwork(networkFile.getAbsolutePath());

		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		new  MatsimVehicleReader(vehicles).readFile(vehiclesFile.getAbsolutePath());

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		for( VehicleType vehicleType : vehicles.getVehicleTypes().values() ){
			carrierVehicleTypes.getVehicleTypes().put( vehicleType.getId(), vehicleType );
		}

		Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, carrierVehicleTypes).readFile(carrierFile.getAbsolutePath());

		EventsManager eventsManager = EventsUtils.createEventsManager();
		MyServiceTrackerEventHandler eventHandler = new MyServiceTrackerEventHandler(vehicles, network, carriers);
		eventsManager.addHandler(eventHandler);
		eventsManager.initProcessing();
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);

		eventsReader.readFile(eventsFile.getAbsolutePath());
		eventsManager.finishProcessing();

		Id<Carrier> carrierId = Id.create( "carrier1", Carrier.class );
		Id<CarrierService> carrierServiceId1 = Id.create( "1", CarrierService.class );
		Id<CarrierService> carrierServiceId11 = Id.create( "11", CarrierService.class );
		Id<CarrierService> carrierServiceId12 = Id.create( "12", CarrierService.class );
		Id<CarrierService> carrierServiceId13 = Id.create( "13", CarrierService.class );
		Id<CarrierService> carrierServiceId14 = Id.create( "14", CarrierService.class );
		Id<CarrierService> carrierServiceId15 = Id.create( "15", CarrierService.class );
		Id<CarrierService> carrierServiceId16 = Id.create( "16", CarrierService.class );

		Id<Person> person_0 = Id.create( "freight_carrier1_veh_carrier_19_lightVehicle_0", Person.class );
		Id<Person> person_1 = Id.create( "freight_carrier1_veh_carrier_19_lightVehicle_1", Person.class );

		LinkedHashMap<Id<Carrier>, ServiceTracker.CarrierServiceTracker> carrierServiceTracker = eventHandler.getServiceTracking().getCarrierServiceTrackers();
		Set<Id<Carrier>> keys = carrierServiceTracker.keySet();
		for(Id<Carrier> key : keys){
			if(key.equals(carrierId)){
				ServiceTracker.CarrierServiceTracker serviceTracker = carrierServiceTracker.get(key);
				System.out.println(serviceTracker.serviceTrackers.get(carrierServiceId1).calculatedArrival);
				Assert.assertEquals("tourETA is not as expected ",Double.valueOf(24032.0),serviceTracker.serviceTrackers.get(carrierServiceId1).calculatedArrival);
				Assert.assertEquals("arrivalTime is not as expected ",Double.valueOf(24405.0),serviceTracker.serviceTrackers.get(carrierServiceId1).arrivalTimeGuess);
				Assert.assertEquals("driverId is not as expected ",person_0,serviceTracker.serviceTrackers.get(carrierServiceId1).driverIdGuess);
				Assert.assertEquals("serviceETA is not as expected ", Double.valueOf(0.0), Double.valueOf(serviceTracker.serviceTrackers.get(carrierServiceId1).expectedArrival));

				Assert.assertEquals("tourETA is not as expected ",Double.valueOf(23766.0),serviceTracker.serviceTrackers.get(carrierServiceId11).calculatedArrival);
				Assert.assertEquals("arrivalTime is not as expected ",Double.valueOf(23777.0),serviceTracker.serviceTrackers.get(carrierServiceId11).arrivalTimeGuess);
				Assert.assertEquals("driverId is not as expected ",person_1,serviceTracker.serviceTrackers.get(carrierServiceId11).driverIdGuess);
				Assert.assertEquals("serviceETA is not as expected ", Double.valueOf(0.0), Double.valueOf(serviceTracker.serviceTrackers.get(carrierServiceId11).expectedArrival));

				Assert.assertEquals("tourETA is not as expected ",Double.valueOf(25566.0),serviceTracker.serviceTrackers.get(carrierServiceId12).calculatedArrival);
				Assert.assertEquals("arrivalTime is not as expected ",Double.valueOf(25945.0),serviceTracker.serviceTrackers.get(carrierServiceId12).arrivalTimeGuess);
				Assert.assertEquals("driverId is not as expected ",person_0,serviceTracker.serviceTrackers.get(carrierServiceId12).driverIdGuess);
				Assert.assertEquals("serviceETA is not as expected ", Double.valueOf(0.0), Double.valueOf(serviceTracker.serviceTrackers.get(carrierServiceId12).expectedArrival));

				Assert.assertEquals("tourETA is not as expected ",Double.valueOf(22533.0),serviceTracker.serviceTrackers.get(carrierServiceId13).calculatedArrival);
				Assert.assertEquals("arrivalTime is not as expected ",Double.valueOf(22538.0),serviceTracker.serviceTrackers.get(carrierServiceId13).arrivalTimeGuess);
				Assert.assertEquals("driverId is not as expected ",person_1,serviceTracker.serviceTrackers.get(carrierServiceId13).driverIdGuess);
				Assert.assertEquals("serviceETA is not as expected ", Double.valueOf(0.0), Double.valueOf(serviceTracker.serviceTrackers.get(carrierServiceId13).expectedArrival));

				Assert.assertEquals("tourETA is not as expected ",Double.valueOf(23066.0),serviceTracker.serviceTrackers.get(carrierServiceId14).calculatedArrival);
				Assert.assertEquals("arrivalTime is not as expected ",Double.valueOf(23434.0),serviceTracker.serviceTrackers.get(carrierServiceId14).arrivalTimeGuess);
				Assert.assertEquals("driverId is not as expected ",person_0,serviceTracker.serviceTrackers.get(carrierServiceId14).driverIdGuess);
				Assert.assertEquals("serviceETA is not as expected ", Double.valueOf(0.0), Double.valueOf(serviceTracker.serviceTrackers.get(carrierServiceId14).expectedArrival));

				Assert.assertEquals("tourETA is not as expected ",Double.valueOf(26399.0),serviceTracker.serviceTrackers.get(carrierServiceId15).calculatedArrival);
				Assert.assertEquals("arrivalTime is not as expected ",Double.valueOf(26782.0),serviceTracker.serviceTrackers.get(carrierServiceId15).arrivalTimeGuess);
				Assert.assertEquals("driverId is not as expected ",person_0,serviceTracker.serviceTrackers.get(carrierServiceId15).driverIdGuess);
				Assert.assertEquals("serviceETA is not as expected ", Double.valueOf(0.0), Double.valueOf(serviceTracker.serviceTrackers.get(carrierServiceId15).expectedArrival));

				Assert.assertEquals("tourETA is not as expected ",Double.valueOf(24732.0),serviceTracker.serviceTrackers.get(carrierServiceId16).calculatedArrival);
				Assert.assertEquals("arrivalTime is not as expected ",Double.valueOf(25108.0),serviceTracker.serviceTrackers.get(carrierServiceId16).arrivalTimeGuess);
				Assert.assertEquals("driverId is not as expected ",person_0,serviceTracker.serviceTrackers.get(carrierServiceId16).driverIdGuess);
				Assert.assertEquals("serviceETA is not as expected ", Double.valueOf(0.0), Double.valueOf(serviceTracker.serviceTrackers.get(carrierServiceId16).expectedArrival));
			}
		}
	}
}