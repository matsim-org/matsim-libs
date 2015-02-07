/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.wagonSim.schedule;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.schedule.OTTDataContainer.Locomotive;
import org.matsim.contrib.wagonSim.schedule.OTTDataContainer.StationData;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

/**
 * @author balmermi
 * @since 2013-07-08
 */
public class OTTDataToMATSimScheduleConverter {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////

	private final Scenario scenario;
	private final ObjectAttributes vehicleAttributes;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public OTTDataToMATSimScheduleConverter(Scenario scenario, ObjectAttributes vehicleAttributes) {
		this.scenario = scenario;
		this.vehicleAttributes = vehicleAttributes;
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	private final void convertStations(OTTDataContainer dataContainer, Network infraNetwork) {
		TransitScheduleFactory factory = scenario.getTransitSchedule().getFactory();

		// get station ids
		Set<Id<TransitStopFacility>> stationIds = new HashSet<>();
		for (Locomotive locomotive : dataContainer.locomotives.values()) {
			for (StationData stationData : locomotive.trips.values()) {
				stationIds.add(stationData.stationId);
			}
		}

		for (Id<TransitStopFacility> stationId : stationIds) {
			Node node = infraNetwork.getNodes().get(Id.create(stationId, Node.class));
			if (node != null) {
				TransitStopFacility stopFacility = factory.createTransitStopFacility(stationId,node.getCoord(),false);
				stopFacility.setName(node.getId().toString());
				scenario.getTransitSchedule().addStopFacility(stopFacility);
			}
			else { throw new RuntimeException("node id="+stationId.toString()+" not found in the network. Bailing out."); }
		}
	}

	//////////////////////////////////////////////////////////////////////
	
	@SuppressWarnings("deprecation")
	private final Date extractStartDate(OTTDataContainer dataContainer, boolean isPerformance) {
		Date startDate = null;
		for (Locomotive locomotive : dataContainer.locomotives.values()) {
			for (StationData stationData : locomotive.trips.values()) {
				if (isPerformance) {
					if (startDate == null) { startDate = new Date(stationData.arrival.getTime()); }
					else if (stationData.arrival.getTime() < startDate.getTime()) { startDate.setTime(stationData.arrival.getTime()); }
				}
				else {
					long time = stationData.arrival.getTime() - (stationData.delayArrival.longValue()*1000L);
					if (startDate == null) { startDate = new Date(time); }
					else if (time < startDate.getTime()) { startDate.setTime(time); }
				}
			}
		}
		
		startDate.setHours(0);
		startDate.setMinutes(0);
		startDate.setSeconds(0);
		return startDate;
	}
	
	//////////////////////////////////////////////////////////////////////
	
	private final void convertSchedules(OTTDataContainer dataContainer, ObjectAttributes trainTypes, boolean isPerformance) {
		TransitScheduleFactory scheduleFactory = scenario.getTransitSchedule().getFactory();
		VehiclesFactory vehiclesFactory = ((ScenarioImpl)scenario).getTransitVehicles().getFactory();
		
		VehicleType vehicleType = vehiclesFactory.createVehicleType(Id.create(WagonSimConstants.DEFAULT_VEHICLE_TYPE, VehicleType.class));
		VehicleCapacity vehicleCapacity = vehiclesFactory.createVehicleCapacity();
		// we do not use this capacity. Therefore it should infinite, otherwise this capacity may exceed
		// before ``our'' capacities are exceeded // dr, oct'13
		vehicleCapacity.setSeats(999999);
		vehicleCapacity.setStandingRoom(999999);
		// we defined the vehicle-enter/leave-time is implicit included in transfer-times which
		// are defined in the transitrouterconfig (for handling see WagonSimTripRouterFactoryImpl#WagonSimRouterWrapper)
		// dr, oct'13
		vehicleType.setAccessTime(0);
		vehicleType.setEgressTime(0);
		vehicleType.setCapacity(vehicleCapacity);
		((ScenarioImpl)scenario).getTransitVehicles().addVehicleType(vehicleType);
		
		Date startDate = extractStartDate(dataContainer,isPerformance);
		System.out.println("startDate="+startDate.toString());

		for (Locomotive locomotive : dataContainer.locomotives.values()) {

			Departure departure = null;
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			for (StationData stationData : locomotive.trips.values()) {

				TransitStopFacility stopFacility = scenario.getTransitSchedule().getFacilities().get(stationData.stationId);
				if (stopFacility == null) { throw new RuntimeException("locomotive id="+locomotive.id+": station id="+stationData.stationId+" not found. Bailing out."); }
				
				double arrivalDelay = Double.NaN;
				double departureDelay = Double.NaN;
				if (departure == null) {
					double lineDepartureOffset = (stationData.departure.getTime()-startDate.getTime())/1000.0;
					if (!isPerformance) { lineDepartureOffset -= stationData.delayDeparture; }
					departure = scheduleFactory.createDeparture(Id.create(locomotive.id, Departure.class), lineDepartureOffset);
					arrivalDelay = 0.0;
				}
				else {
					arrivalDelay = (stationData.arrival.getTime()-startDate.getTime())/1000.0 - departure.getDepartureTime();
					if (!isPerformance) { arrivalDelay -= stationData.delayArrival; }
				}
				departureDelay = (stationData.departure.getTime()-startDate.getTime())/1000.0 - departure.getDepartureTime();
				if (!isPerformance) { departureDelay -= stationData.delayDeparture; }
				

				if (departureDelay < arrivalDelay) { throw new RuntimeException("locomotive id="+locomotive.id+": arrival="+stationData.arrival.toString()+" does not fit with departure="+stationData.departure.toString()+". ("+departureDelay+"<"+arrivalDelay+") Bailing out."); }

				TransitRouteStop stop = scheduleFactory.createTransitRouteStop(stopFacility,arrivalDelay,departureDelay);
				stop.setAwaitDepartureTime(true);
				transitRouteStops.add(stop);
			}
			
			if (transitRouteStops.size() > 1) {
				
				// check if train type is given
				if (trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_SPEED) == null) {
					throw new RuntimeException("locomotive id="+locomotive.id+": type="+locomotive.type+" is not defined by the train type table. Bailing out.");
				}
				
				TransitLine line = scheduleFactory.createTransitLine(Id.create(locomotive.id, TransitLine.class));
				scenario.getTransitSchedule().addTransitLine(line);
				TransitRoute route = scheduleFactory.createTransitRoute(Id.create(line.getId(), TransitRoute.class), null, transitRouteStops, TransportMode.pt);
				line.addRoute(route);
				
				Vehicle vehicle = vehiclesFactory.createVehicle(Id.create(route.getId(), Vehicle.class), vehicleType);
				((ScenarioImpl)scenario).getTransitVehicles().addVehicle(vehicle);
				departure.setVehicleId(vehicle.getId());
				route.addDeparture(departure);
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_TYPE, locomotive.type);
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_SPEED, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_SPEED));
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_WEIGHT, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_WEIGHT));
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_LENGTH, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_LENGTH));

				// the next day
				vehicle = vehiclesFactory.createVehicle(Id.create(route.getId()+".1", Vehicle.class),vehicleType);
				((ScenarioImpl)scenario).getTransitVehicles().addVehicle(vehicle);
				departure = scheduleFactory.createDeparture(Id.create(vehicle.getId(), Departure.class), departure.getDepartureTime()+24*3600);
				departure.setVehicleId(vehicle.getId());
				route.addDeparture(departure);
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_TYPE, locomotive.type);
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_SPEED, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_SPEED));
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_WEIGHT, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_WEIGHT));
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_LENGTH, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_LENGTH));

				// the day after the next day
				vehicle = vehiclesFactory.createVehicle(Id.create(route.getId()+".2", Vehicle.class),vehicleType);
				((ScenarioImpl)scenario).getTransitVehicles().addVehicle(vehicle);
				departure = scheduleFactory.createDeparture(Id.create(vehicle.getId(), Departure.class), departure.getDepartureTime()+24*3600);
				departure.setVehicleId(vehicle.getId());
				route.addDeparture(departure);
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_TYPE, locomotive.type);
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_SPEED, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_SPEED));
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_WEIGHT, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_WEIGHT));
				this.vehicleAttributes.putAttribute(vehicle.getId().toString(),WagonSimConstants.TRAIN_MAX_LENGTH, trainTypes.getAttribute(locomotive.type.toString(),WagonSimConstants.TRAIN_MAX_LENGTH));
			}
			else if (transitRouteStops.size() == 1) {
				System.out.println("locomotive id="+locomotive.id+": only one station given. Therefore, no transitLine created.");
			}
			else {
				System.out.println("locomotive id="+locomotive.id+": no station is given. Therefore, no transitLine created.");
			}
		}
	}

	//////////////////////////////////////////////////////////////////////

	private final void convertPseudoNetwork() {
		List<Id<Node>> nodeIds = new ArrayList<Id<Node>>(scenario.getNetwork().getNodes().keySet());
		for (Id<Node> id : nodeIds) { scenario.getNetwork().removeNode(id); }
		new CreatePseudoNetwork(scenario.getTransitSchedule(),scenario.getNetwork(),"").createNetwork();

		for (Link l : scenario.getNetwork().getLinks().values()) {
			l.setCapacity(WagonSimConstants.DEFAULT_CAPACITY);
			l.setFreespeed(WagonSimConstants.DEFAULT_FREESPEED);
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	
	private final void createVehicleLinkSpeedAttributes() {
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				Set<Id<Vehicle>> vehIds = new HashSet<>();
				for (Departure departure : transitRoute.getDepartures().values()) {
					vehIds.add(departure.getVehicleId());
				}
				
				Iterator<TransitRouteStop> iterator = transitRoute.getStops().iterator();
				double departure = iterator.next().getDepartureOffset();
				while (iterator.hasNext()) {
					TransitRouteStop routeStop = iterator.next();
					double arrival = routeStop.getArrivalOffset();
					Link link = scenario.getNetwork().getLinks().get(routeStop.getStopFacility().getLinkId());
					double speed = link.getLength() / (arrival - departure);
					if (speed >= 200.0) {
						System.out.println("line="+transitLine.getId()+";route="+transitRoute.getId()+"stop="+routeStop.getStopFacility().getId()+": lLenth="+link.getLength()+";arr="+arrival+";dep="+departure+"");
					}

					for (Id<Vehicle> vehId : vehIds) {
						this.vehicleAttributes.putAttribute(vehId.toString(),link.getId().toString(),speed);
					}
					
					departure = routeStop.getDepartureOffset();
				}
			}
		}
	}

	//////////////////////////////////////////////////////////////////////
	
	public final void convert(OTTDataContainer dataContainer, Network infraNetwork, ObjectAttributes trainTypes, boolean isPerformance) {
		convertStations(dataContainer,infraNetwork);
		convertSchedules(dataContainer,trainTypes,isPerformance);
		convertPseudoNetwork();
		createVehicleLinkSpeedAttributes();
	}
}
