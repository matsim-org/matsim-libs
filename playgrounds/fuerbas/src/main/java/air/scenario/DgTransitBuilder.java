/* *********************************************************************** *
 * project: org.matsim.*
 * DgTransitBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import air.scenario.oag.DgOagFlight;
import air.scenario.oag.DgOagFlightsData;


public class DgTransitBuilder {

	
	private static final double MACH_2 = 686.0;
	
	private ScenarioImpl scenario;

	public DgTransitBuilder(ScenarioImpl scenario) {
		this.scenario = scenario;
	}

	public void createFacilities(Map<Id<Node>, SfMatsimAirport> airportMap){
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();
		
		for (SfMatsimAirport airport : airportMap.values()){
			TransitStopFacility transitStopFacility = sf.createTransitStopFacility(Id.create(airport.getId(), TransitStopFacility.class), airport.coordApronEnd, false);
			transitStopFacility.setLinkId(airport.getStopFacilityLinkId());
			schedule.addStopFacility(transitStopFacility);
		}
	}
	
	public void createSchedule(DgOagFlightsData flightsData, Map<Id<Node>, SfMatsimAirport> airportMap) {
		this.createFacilities(airportMap);
		
		Vehicles veh = this.scenario.getTransitVehicles();
		TransitSchedule schedule = this.scenario.getTransitSchedule();
		TransitScheduleFactory sf = schedule.getFactory();
		
		for (DgOagFlight flight : flightsData.getFlightDesignatorFlightMap().values()){
			Id<TransitLine> lineId = Id.create(flight.getOriginCode() + "_" + flight.getDestinationCode() + "_" + flight.getCarrier(), TransitLine.class);
			TransitLine line = schedule.getTransitLines().get(lineId);
			if (line == null) {
				line = sf.createTransitLine(lineId);
				schedule.addTransitLine(line);
			}
			Id<Link> id = Id.create(flight.getOriginCode() + "_" + flight.getDestinationCode() + "_" + flight.getFlightDesignator(), Link.class);

			Id<Link> fromId = Id.create(flight.getOriginCode(), Link.class);
			Id<Link> toId = Id.create(flight.getDestinationCode(), Link.class);
			TransitStopFacility fromFacility = schedule.getFacilities().get(fromId);
			TransitStopFacility toFacility = schedule.getFacilities().get(toId);
			TransitRouteStop fromStop = sf.createTransitRouteStop(fromFacility, 0.0, 0.0);
			TransitRouteStop toStop = sf.createTransitRouteStop(toFacility, flight.getScheduledDuration(), flight.getScheduledDuration());
			List<TransitRouteStop> stopList = new ArrayList<TransitRouteStop>();
			stopList.add(fromStop);
			stopList.add(toStop);
			
			NetworkRoute route = new LinkNetworkRouteImpl(fromId, toId);
			SfMatsimAirport fromAirport = airportMap.get(fromId);
			SfMatsimAirport toAirport = airportMap.get(toId);
			List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
			routeLinkIds.addAll(fromAirport.getDepartureLinkIdList());
			routeLinkIds.add(id);
			routeLinkIds.addAll(toAirport.getArrivalLinkIdList());
			route.setLinkIds(fromId, routeLinkIds, toId);

			TransitRoute transitRoute = sf.createTransitRoute(Id.create(id, TransitRoute.class), route, stopList, TransportMode.pt);
			line.addRoute(transitRoute);

			Departure departure = sf.createDeparture(Id.create(id, Departure.class), flight.getDepartureTime());
			Id<Vehicle> vehicleId = Id.create(flight.getFlightDesignator(), Vehicle.class);
			departure.setVehicleId(vehicleId);
			transitRoute.addDeparture(departure);
			
			Id<VehicleType> typeId = Id.create(flight.getAircraftType() + "_" + flight.getCarrier() + "_" + flight.getSeatsAvailable(), VehicleType.class);
			VehicleType vehType = veh.getVehicleTypes().get(typeId);
			if ( vehType == null ){
				vehType = veh.getFactory().createVehicleType(typeId);
				VehicleCapacity cap = veh.getFactory().createVehicleCapacity();
				cap.setSeats(flight.getSeatsAvailable());
				vehType.setCapacity( cap);
				vehType.setMaximumVelocity(MACH_2);
				veh.addVehicleType(vehType);
			}
			Vehicle vehicle = veh.getFactory().createVehicle(vehicleId, vehType);
			veh.addVehicle( vehicle);
		}
		
	}

	public void writeTransitFile(String scheduleFilename, String vehicleFilename) {
		TransitScheduleWriterV1 scheduleWriter = new TransitScheduleWriterV1(this.scenario.getTransitSchedule());
		scheduleWriter.write(scheduleFilename);
		
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(this.scenario.getTransitVehicles());
		vehicleWriter.writeFile(vehicleFilename);
	}

}
