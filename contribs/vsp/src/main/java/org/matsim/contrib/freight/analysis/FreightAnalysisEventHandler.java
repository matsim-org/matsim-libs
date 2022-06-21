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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.LSPServiceStartEvent;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;
import org.matsim.contrib.freight.events.ShipmentPickedUpEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceStartEventHandler;
import org.matsim.contrib.freight.events.eventhandler.ShipmentDeliveredEventHandler;
import org.matsim.contrib.freight.events.eventhandler.ShipmentPickedUpEventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/*
 * EventHandler for analysis of matsim-freight runs. Tracks freight vehicles, carriers, shipments and services and is able to export results to TSV files.
 * Only uses information that is certain by default. Without LSP Events this means that the connection between Carrier-related Objects (Carriers, Shipments, Services) often cannot be made, but this Handles tries to make an educated guess which you can optionally include in the export. Guessed info will be preceeded by "##" in export.
 *
 * @author Jakob Harnisch (MATSim advanced class 2020/21)
 * */

class FreightAnalysisEventHandler implements  ActivityStartEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ShipmentPickedUpEventHandler, ShipmentDeliveredEventHandler, LSPServiceStartEventHandler, LSPServiceEndEventHandler {
	private final static Logger log = Logger.getLogger(FreightAnalysisEventHandler.class);
	private final Vehicles vehicles;
	private final Network network;
	private final Carriers carriers;
	private Integer iterationCount=0;
	private FreightAnalysisVehicleTracking vehicleTracking = new FreightAnalysisVehicleTracking();
	private FreightAnalysisShipmentTracking shipmentTracking = new FreightAnalysisShipmentTracking();
	private FreightAnalysisServiceTracking serviceTracking = new FreightAnalysisServiceTracking();

	public FreightAnalysisEventHandler(Network network, Vehicles vehicles, Carriers carriers) {
		this.network = network;
		this.carriers = carriers;
		this.vehicles = vehicles;
		this.init();
	}

	// create all trackers and estimate deliveryTimes
	private void init(){
		// the EventHandler tracks all vehicles containing "freight" by default which is as of now (02/21) the easiest way to do so, but not a pretty one.
		// You can add trackers by yourself at will.
		for (Vehicle vehicle : vehicles.getVehicles().values()) {
			String vehicleIdString = vehicle.getId().toString();
			if (vehicle.getId().toString().contains("freight")) {
				vehicleTracking.addTracker(vehicle);

				// The vehicleId is based on a scheme like "freight_" + <carrierId> + "_veh" + <vehicleNumber>
				// we try to extract the CarrierId based on this scheme and see if it is indeed a carrier. TODO: Where do we check this? KMT Mai22
				// If that is the case, it is used as a guess for the Carrier.
				String carrierGuess = vehicleIdString.replaceAll("_veh.*","");
				carrierGuess = carrierGuess.replaceAll("freight_", "");
				for (Carrier carrier: carriers.getCarriers().values()){
					if (carrier.getId().toString().equals(carrierGuess)){
						vehicleTracking.addCarrierGuess(vehicle.getId(),carrier.getId());
					}
				}
				log.info("started tracking vehicle #" + vehicle.getId().toString());
			}
		}

		for (Carrier carrier : carriers.getCarriers().values()) {
			// for all shipments and services of the carriers, tracking is started here.
			for (CarrierShipment shipment : carrier.getShipments().values()) {
				shipmentTracking.addTracker(shipment);
			}
			for (CarrierService service : carrier.getServices().values()) {
				serviceTracking.addTracker(service, carrier.getId());
			}
		}
		serviceTracking.estimateArrivalTimes(carriers);
	}


	// evaluating the ActivityEvents to track vehicle usage and guess what happens with services and shipments
	@Override
	public void handleEvent(ActivityStartEvent activityStartEvent) {
		if (activityStartEvent.getActType().equals("end")) {
			vehicleTracking.endVehicleUsage(activityStartEvent.getPersonId());
		}
		if (activityStartEvent.getActType().equals("service")) {
			serviceTracking.trackServiceActivityStart(activityStartEvent);
		}

		if (activityStartEvent.getActType().equals("delivery")) {
			shipmentTracking.trackDeliveryActivity(activityStartEvent);
		}

		if (activityStartEvent.getActType().equals("pickup")){
			shipmentTracking.trackPickupActivity(activityStartEvent);
		}

	}

	// link events are used to calculate vehicle travel time and distance
	@Override
	public void handleEvent(LinkEnterEvent linkEnterEvent) {
		vehicleTracking.trackLinkEnterEvent(linkEnterEvent);
	}

	@Override
	public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
		vehicleTracking.trackLinkLeaveEvent(linkLeaveEvent, network.getLinks().get(linkLeaveEvent.getLinkId()).getLength());
	}

	// Person<>Vehicle relations and vehicle usage times are tracked
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleTracking.addDriver2Vehicle(event.getPersonId(), event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		vehicleTracking.registerVehicleLeave(event);
	}

	// LSP Events for Shipments and Services, those are UNTESTED
	@Override
	public void handleEvent(ShipmentDeliveredEvent event) {
		shipmentTracking.trackDeliveryEvent(event);
	}

	@Override
	public void handleEvent(ShipmentPickedUpEvent event) {
		shipmentTracking.trackPickedUpEvent(event);
		// as we know the driver of the shipment now, we can assign the shipment's carrier to the driver's vehicle.
		if (shipmentTracking.getShipments().containsKey(event.getShipment().getId()) && vehicleTracking.getDriver2VehicleId(event.getDriverId())!=null){
			vehicleTracking.addCarrier2Vehicle(vehicleTracking.getDriver2VehicleId(event.getDriverId()), shipmentTracking.getShipments().get(event.getShipment()).carrierId);
		}
	}

	@Override
	public void handleEvent(LSPServiceEndEvent event) {
		serviceTracking.handleEndEvent(event);
	}

	@Override
	public void handleEvent(LSPServiceStartEvent event) {
		serviceTracking.handleStartEvent(event);
		// as we know the driver of a service now, we can assign the shipment's carrier to the driver's vehicle.
		if (serviceTracking.getCarrierServiceTrackers().containsKey(event.getService().getId()) && vehicleTracking.getDriver2VehicleId(event.getDriverId())!=null){
			vehicleTracking.addCarrier2Vehicle(vehicleTracking.getDriver2VehicleId(event.getDriverId()), serviceTracking.getCarrierServiceTrackers().get(event.getService()).carrierId);
		}
	}


	// ##################################################
	// ########         Export methods           ########
	// ##################################################

	// Export vehicle Statistics to single TSV
//	public void exportVehicleInfo(String path){
//		exportVehicleInfo(path, false);
//	}

	public void exportVehicleInfo(String path, Boolean exportGuesses) {
//		path = getIterationDirectory(path);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/freightVehicleStats.tsv"));
			LinkedHashMap<String, LinkedHashSet<String>> carrierVehicleStatistics = new LinkedHashMap<>();
			out.write("vehicleId	vehicleType	carrierId	driverID	usageTime	roadTime	travelDistance	vehicleCost	tripCount");
			out.newLine();
			LinkedHashMap<Id<Vehicle>, VehicleTracker> trackers = vehicleTracking.getTrackers();
			for (Id<Vehicle> vehId : trackers.keySet()) {
				VehicleTracker tracker = trackers.get(vehId);
				String lastDriverIdString = id2String(tracker.lastDriverId);
				// if the carrier is not certain, export the guess if that is wanted.
				String carrierIdString = (tracker.carrierId == null && exportGuesses) ? "##" + id2String(tracker.carrierIdGuess) : id2String(tracker.carrierId);
				// vehicle statistics are collected per carrier...
				if (!carrierVehicleStatistics.containsKey(carrierIdString)) {
					carrierVehicleStatistics.put(carrierIdString, new LinkedHashSet<>());
				}
				String vehicleInfoString = vehId.toString() + "	" + tracker.vehicleType.getId().toString() + "	" + carrierIdString + "	" + lastDriverIdString + "	" + tracker.usageTime.toString() + "	" + tracker.roadTime.toString() + "	" + tracker.travelDistance.toString() + "	" + tracker.cost.toString() + "	" + tracker.tripHistory.size();
				carrierVehicleStatistics.get(carrierIdString).add(vehicleInfoString);
			}
			// ...and then written into individual and a single file.
			for (String carrierIdString : carrierVehicleStatistics.keySet()){
				BufferedWriter carrierOutFile = new BufferedWriter(new FileWriter(path + "/carrier_" + carrierIdString + "_vehicleStats.tsv"));
				carrierOutFile.write("vehicleId	vehicleType	carrierId	driverID	usageTime	roadTime	travelDistance	vehicleCost	tripCount");
				for(String line: carrierVehicleStatistics.get(carrierIdString)){
					carrierOutFile.write(line);
					carrierOutFile.newLine();
					out.write(line);
					out.newLine();
				}
				carrierOutFile.close();
			}
			out.close();
			System.out.println("File created successfully");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Export Vehicle Statistics per Trip to TSV
//	public void exportVehicleTripInfo(String path){
//		exportVehicleTripInfo(path, false );
//	}

	public void exportVehicleTripInfo(String path, Boolean exportGuesses) {
//		path = getIterationDirectory(path);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/freightVehicleTripStats.tsv"));
			out.write("vehicleId	vehicleType	tripNumber	carrierId	driverId	tripRoadTime	tripDistance	tripVehicleCost");
			out.newLine();
			LinkedHashMap<Id<Vehicle>, VehicleTracker> trackers = vehicleTracking.getTrackers();
			LinkedHashMap<String, LinkedHashSet<String>> carrierTripStatistics = new LinkedHashMap<>();
			for (Id<Vehicle> vehId : trackers.keySet()) {
				VehicleTracker tracker = trackers.get(vehId);
				int i = 0;
				for (VehicleTracker.VehicleTrip trip : tracker.tripHistory) {
					// if info is not certain, export the guess if that is wanted.
					String driverIdString = trip.driverId == null ? "" : trip.driverId.toString();
					String carrierIdString = ((tracker.carrierId == null) && exportGuesses) ? ("##" + id2String(tracker.carrierIdGuess)) : id2String(tracker.carrierId);
					// trip statistics are collected per carrier...
					if (!carrierTripStatistics.containsKey(carrierIdString)) {
						carrierTripStatistics.put(carrierIdString, new LinkedHashSet<>());
					}
					String vehicleTripInfoString = vehId.toString() + "	" + tracker.vehicleType.getId().toString() + "	" + i + "	" + carrierIdString + "	" + driverIdString + "	" + trip.travelTime + "	" + trip.travelDistance + "	" + trip.cost;
					carrierTripStatistics.get(carrierIdString).add(vehicleTripInfoString);
					i++;
				}
			}
			// ...and then written into individual and a single file.
			for (String carrierIdString : carrierTripStatistics.keySet()){
				BufferedWriter carrierOutFile = new BufferedWriter(new FileWriter(path + "/carrier_" + carrierIdString + "_tripStats.tsv"));
				carrierOutFile.write("vehicleId	vehicleType	tripNumber	carrierId	driverId	tripRoadTime	tripDistance	tripVehicleCost");
				carrierOutFile.newLine();
				for(String line: carrierTripStatistics.get(carrierIdString)){
					carrierOutFile.write(line);
					carrierOutFile.newLine();
					out.write(line);
					out.newLine();
				}
				carrierOutFile.close();
			}
			out.close();
			System.out.println("File created successfully");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// Export Vehicle Statistics grouped by VehicleType to individual TSV files per carrier
//	public void exportVehicleTypeStats(String path){
//		//there are no guesses to be exported as of now, still having this method to keep the export calls consistent
//		exportVehicleTypeStats(path, false);
//	}
	public void exportVehicleTypeStats(String path, Boolean exportGuesses) {
//		path = getIterationDirectory(path);
		try {
			BufferedWriter singleFile = new BufferedWriter(new FileWriter(path + "/carrierStats.tsv"));
			singleFile.write("carrierId	vehicleType	vehicleCount	totalDistance	totalServiceTime	totalRoadTime	totalCost");
			singleFile.newLine();
			for (Carrier carrier : carriers.getCarriers().values()) {
				LinkedHashMap<String, CarrierVehicleTypeStats> vehicleTypeStatsMap = new LinkedHashMap<>();
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/carrier_" + carrier.getId().toString() + "_VehicleTypeStats.tsv"));
				for (VehicleTracker tracker : vehicleTracking.getTrackers().values()) {
					// if desired get carrierIdString, in which case the vehicleType gets the "##" prefix to separate guessed vehicle connections from non-guessed ones, even if they are of the same vehicle type
					String carrierIdString = tracker.carrierId==null && exportGuesses ? id2String(tracker.carrierIdGuess) : id2String(tracker.carrierId);
					String vehicleTypeString = tracker.carrierId==null && exportGuesses ? "##" + tracker.vehicleType.getId().toString() : tracker.vehicleType.getId().toString();

					if (carrierIdString.equals(id2String(carrier.getId()))) {
						if (!vehicleTypeStatsMap.containsKey(vehicleTypeString)) {
							vehicleTypeStatsMap.put(vehicleTypeString, new CarrierVehicleTypeStats());
						}
						CarrierVehicleTypeStats cVtStTr = vehicleTypeStatsMap.get(vehicleTypeString);
						cVtStTr.vehicleCount++;
						cVtStTr.totalCost += tracker.cost;
						cVtStTr.totalDistance += tracker.travelDistance;
						cVtStTr.totalRoadTime += tracker.roadTime;
						cVtStTr.totalServiceTime += tracker.usageTime;
					}
				}
				out.write("carrierId	vehicleType	vehicleCount	totalDistance	totalServiceTime	totalRoadTime	totalCost");
				out.newLine();
				for (String vt : vehicleTypeStatsMap.keySet()) {
					CarrierVehicleTypeStats vts = vehicleTypeStatsMap.get(vt);
					out.write(carrier.getId().toString() +"	" + vt + "	" + vts.vehicleCount.toString() + "	" + vts.totalDistance.toString() + "	" + vts.totalServiceTime + "	" + vts.totalRoadTime + "	" + vts.totalCost);
					singleFile.write(carrier.getId().toString() + "	" + vt + "	" + vts.vehicleCount.toString() + "	" + vts.totalDistance.toString() + "	" + vts.totalServiceTime + "	" + vts.totalRoadTime + "	" + vts.totalCost);
					out.newLine();
					singleFile.newLine();
				}
				out.close();
			}
			singleFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	// Export Statistics of performed services
//	public void exportServiceInfo(String path){
//		exportServiceInfo(path, false);
//	}
//
	public void exportServiceInfo(String path, Boolean exportGuesses) {
//		path = getIterationDirectory(path);
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(path + "/serviceStats.tsv"));
			out.write("carrierId	serviceId	driverId	vehicleId	serviceETA	tourETA 	arrivalTime");
			out.newLine();
			LinkedHashMap<Id<Carrier>, ServiceTracker.CarrierServiceTracker> carrierServiceTrackers= serviceTracking.getCarrierServiceTrackers();
			for(ServiceTracker.CarrierServiceTracker carrierServiceTracker:carrierServiceTrackers.values()){
				String carrierIdString = id2String(carrierServiceTracker.carrierId);
				BufferedWriter out_carrier = new BufferedWriter(new FileWriter(path+"/carrier_" + carrierIdString + "_ServiceStats.tsv"));
				out_carrier.write("carrierId	serviceId	driverId	vehicleId	serviceETA	tourETA 	arrivalTime");
				out_carrier.newLine();
				for (ServiceTracker serviceTracker : carrierServiceTracker.serviceTrackers.values()){
					String serviceIdString = id2String(serviceTracker.service.getId());
					// if info is not certain, export the guess if that is wanted.
					String driverIdString = (exportGuesses && serviceTracker.driverId == null) ? "##" + id2String(serviceTracker.driverIdGuess) : id2String(serviceTracker.driverId);
					String vehicleIdString = (vehicleTracking.getDriver2VehicleId(serviceTracker.driverId) == null && exportGuesses) ? "##" + id2String(vehicleTracking.getDriver2VehicleId(serviceTracker.driverIdGuess)) : id2String(vehicleTracking.getDriver2VehicleId(serviceTracker.driverId));
					String arrivalTime = (exportGuesses && serviceTracker.startTime == 0.0) ? "?" + serviceTracker.arrivalTimeGuess.toString() : serviceTracker.startTime.toString();
					out.write(carrierIdString + "	" + serviceIdString + "	" + driverIdString + "	" + vehicleIdString + "	" + serviceTracker.expectedArrival + "	" + serviceTracker.calculatedArrival + "	" + arrivalTime);
					out_carrier.write(carrierIdString + "	" + serviceIdString + "	" + driverIdString + "	" + vehicleIdString + "	" + serviceTracker.expectedArrival + "	" + serviceTracker.calculatedArrival + "	" + arrivalTime);
					out.newLine();
					out_carrier.newLine();
				}
				out_carrier.close();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Export Info about Shipments to individual and per-carrier TSV
//	public void exportShipmentInfo(String path){
//		exportShipmentInfo(path, false);
//	}

	public void exportShipmentInfo(String path, Boolean exportGuesses) {
//		path = getIterationDirectory(path);
		try {
			BufferedWriter singleFile = new BufferedWriter(new FileWriter(path + "/shipmentStats.tsv"));
			singleFile.write("carrierId	shipmentId	driverId	vehicleId	pickupTime	deliveryTime	deliveryDuration	beelineDistance");
			singleFile.newLine();
			for (Carrier carrier : carriers.getCarriers().values()) {
				BufferedWriter out = new BufferedWriter(new FileWriter(path + "/carrier_" + carrier.getId().toString() + "_ShipmentStats.tsv"));
				out.write("carrierId	shipmentId	driverId	vehicleId	pickupTime	deliveryTime	deliveryDuration	beelineDistance");
				out.newLine();
				for (CarrierShipment shipment : carrier.getShipments().values()) {
					ShipmentTracker shipmentTracker = shipmentTracking.getShipments().get(shipment.getId());
					if (shipmentTracker == null) {
						continue;
					}
					Id<Link> from = shipment.getFrom();
					Id<Link> toLink = shipment.getTo();
					// if info is not certain, export the guess if that is wanted.
					String carrierIdString = id2String(carrier.getId());
					String shipmentIdString = id2String(shipment.getId());
					String driverIdString = (shipmentTracker.driverId == null && exportGuesses) ? "##" + id2String(shipmentTracker.driverIdGuess) : id2String(shipmentTracker.driverId);
					String vehicleIdString = (vehicleTracking.getDriver2VehicleId(shipmentTracker.driverId) == null && exportGuesses) ? "##" + id2String(vehicleTracking.getDriver2VehicleId(shipmentTracker.driverIdGuess)) : id2String(vehicleTracking.getDriver2VehicleId(shipmentTracker.driverId));
					// calculate euclidean Distance between from and to for comparison
					double dist = NetworkUtils.getEuclideanDistance(network.getLinks().get(from).getCoord(), network.getLinks().get(toLink).getCoord());
					out.write(carrierIdString + "	" + shipment.getId().toString() + "	" + driverIdString + "	" + vehicleIdString + "	"  + shipmentTracker.pickUpTime.toString() + "	" + shipmentTracker.deliveryTime.toString() + "	" + shipmentTracker.deliveryDuration.toString() + "	" +dist);
					out.newLine();
					singleFile.write(carrierIdString + "	" + shipmentIdString + "	" + driverIdString + "	" + vehicleIdString + "	"  + shipmentTracker.pickUpTime.toString() + "	" + shipmentTracker.deliveryTime.toString() + "	" + shipmentTracker.deliveryDuration.toString() + "	" + dist);
					singleFile.newLine();
				}
				out.close();
			}
			singleFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

//	private String getIterationDirectory(String parentDirectory) {
//		String path = parentDirectory + "/freight-analysis-it_" + iterationCount.toString(); // create one subfolder per iteration
//		try {
//			Files.createDirectories(Paths.get(path));
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return(path);
//	}

	// reset the EventHandler, would typically be done between iterations of a simulation
	public void reset(){
		iterationCount++;
		this.vehicleTracking = new FreightAnalysisVehicleTracking();
		this.shipmentTracking = new FreightAnalysisShipmentTracking();
		this.serviceTracking = new FreightAnalysisServiceTracking();
		init();
	}

	private String id2String(Id id){ //Failsafe Id to String - Converter, because Id.toString() throws Exception if the Id is null.
		return id==null?" ":id.toString(); // return space because instead of empty string because TSV files get confused otherwise
	}

	private static class CarrierVehicleTypeStats {
		Integer vehicleCount=0;
		Double totalDistance=0.0;
		Double totalRoadTime=0.0;
		Double totalServiceTime=0.0;
		Double totalCost=0.0;
	}
}