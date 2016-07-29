/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.andreas.P.init;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleWriterV1;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

@Deprecated
public class CreateInitialTimeSchedule {
	
	private Network net;
	private Vehicles veh;
	private TransitSchedule tS;
	private int numberOfAgents;
	
	public static void createInitialTimeSchedule(PConfigGroup pConfig){
		Coord minXY = new Coord(pConfig.getMinX(), pConfig.getMinY());
		Coord maxXY = new Coord(pConfig.getMaxX(), pConfig.getMaxY());
		
		CreateInitialTimeSchedule cITS = new CreateInitialTimeSchedule(pConfig.getNetwork(), pConfig.getGridDistance(), minXY, maxXY, pConfig.getNumberOfAgents());
		cITS.run();
		cITS.writeTransitSchedule(pConfig.getCurrentOutputBase() + "transitSchedule.xml");
		cITS.writeVehicles(pConfig.getCurrentOutputBase() + "transitVehicles.xml");
	}
	
	public static TransitLine createSingleTransitLine(Network net, TransitSchedule transitSchedule, Id<Vehicle> driverId){
		CreateInitialTimeSchedule cITS = new CreateInitialTimeSchedule(net, transitSchedule);
		return cITS.createSingleTransitLine(driverId);
	}

	public CreateInitialTimeSchedule(String networkInFile, double gridDistance, Coord minXY, Coord maxXY, int numberOfAgents) {
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(sc.getNetwork());
		netReader.readFile(networkInFile);		
		this.net = (Network) sc.getNetwork();
		
		this.tS = CreateStops.createStops(this.net, gridDistance, minXY, maxXY);
		this.veh = VehicleUtils.createVehiclesContainer();
		this.numberOfAgents = numberOfAgents;
	}

	public CreateInitialTimeSchedule(Network net, TransitSchedule transitSchedule) {
		this.net = net;
		this.tS = transitSchedule;
	}

	private void run() {
		for (int i = 0; i < this.numberOfAgents; i++) {
			
			// initialize
			Id<Vehicle> driverId = Id.create("free_" + i, Vehicle.class);
			this.tS.addTransitLine(createSingleTransitLine(driverId));
		}
	}

	public TransitLine createSingleTransitLine(Id<Vehicle> driverId){
		// initialize
		TransitLine line = this.tS.getFactory().createTransitLine(Id.create(driverId, TransitLine.class));			
		TransitStopFacility startStop = getRandomTransitStop();			
		TransitStopFacility endStop = getRandomTransitStop();
		
		TransitRoute transitRoute_H = createRoute(Id.create(driverId + "_H", TransitRoute.class), startStop, endStop);
		TransitRoute transitRoute_R = createRoute(Id.create(driverId + "_R", TransitRoute.class), endStop, startStop);
		
		// register route
		line.addRoute(transitRoute_H);
		line.addRoute(transitRoute_R);
		
		// add departures
		int n = 0;
		for (double j = 0.0; j < 24 * 3600; ) {
			Departure departure = this.tS.getFactory().createDeparture(Id.create(n, Departure.class), j);
			departure.setVehicleId(driverId);
			transitRoute_H.addDeparture(departure);
			j += transitRoute_H.getStop(endStop).getDepartureOffset() + 5 *60;
			n++;
			
			departure = this.tS.getFactory().createDeparture(Id.create(n, Departure.class), j);
			departure.setVehicleId(driverId);
			transitRoute_R.addDeparture(departure);
			j += transitRoute_R.getStop(startStop).getDepartureOffset() + 5 *60;
			n++;
		}
		
		return line;
	}

	private TransitRoute createRoute(Id<TransitRoute> routeID, TransitStopFacility startStop, TransitStopFacility endStop){
		
		FreespeedTravelTimeAndDisutility tC = new FreespeedTravelTimeAndDisutility(-6.0, 0.0, 0.0);
		LeastCostPathCalculator routingAlgo = new Dijkstra(this.net, tC, tC);
		
		Node startNode = this.net.getLinks().get(startStop.getLinkId()).getToNode();
		Node endNode = this.net.getLinks().get(endStop.getLinkId()).getFromNode();
		
		int startTime = 0 * 3600;
		
		// get Route
		Path path = routingAlgo.calcLeastCostPath(startNode, endNode, startTime, null, null);
		NetworkRoute route = new LinkNetworkRouteImpl(startStop.getLinkId(), endStop.getLinkId());
		route.setLinkIds(startStop.getLinkId(), NetworkUtils.getLinkIds(path.links), endStop.getLinkId());
		
		
		// get stops at Route
		List<TransitRouteStop> stops = new LinkedList<TransitRouteStop>();
							
		// first stop
		TransitRouteStop routeStop = this.tS.getFactory().createTransitRouteStop(startStop, startTime, startTime);
		stops.add(routeStop);
		
		// additional stops
		for (Link link : path.links) {
			startTime += link.getLength() / link.getFreespeed();
			if(this.tS.getFacilities().get(link.getId()) == null){
				continue;
			}
			routeStop = this.tS.getFactory().createTransitRouteStop(this.tS.getFacilities().get(link.getId()), startTime, startTime);
			stops.add(routeStop);
		}
		
		// last stop
		startTime += this.net.getLinks().get(endStop.getLinkId()).getLength() / this.net.getLinks().get(endStop.getLinkId()).getFreespeed();
		routeStop = this.tS.getFactory().createTransitRouteStop(endStop, startTime, startTime);
		stops.add(routeStop);
		
		// register departure
		TransitRoute transitRoute = this.tS.getFactory().createTransitRoute(routeID, route, stops, "pt");

		
		return transitRoute;
	}
	
	private void writeVehicles(String vehiclesOutFile) {
		VehiclesFactory vehFactory = this.veh.getFactory();
		VehicleType vehType = vehFactory.createVehicleType(Id.create("defaultTransitVehicleType", VehicleType.class));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(8));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehType.setCapacity(capacity);
		this.veh.addVehicleType(vehType);
	
		for (TransitLine line : this.tS.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicle = vehFactory.createVehicle(departure.getVehicleId(), vehType);
					this.veh.addVehicle( vehicle);
				}
			}
		}
		
		VehicleWriterV1 writer = new VehicleWriterV1(this.veh);
		writer.writeFile(vehiclesOutFile);		
	}

	private void writeTransitSchedule(String transitScheduleOutFile) {
		TransitScheduleWriterV1 writer = new TransitScheduleWriterV1(this.tS);
		writer.write(transitScheduleOutFile);
	}

	private TransitStopFacility getRandomTransitStop(){
		int i = this.tS.getFacilities().size();
		for (TransitStopFacility stop : this.tS.getFacilities().values()) {
			if(MatsimRandom.getRandom().nextDouble() < 1.0 / i){
				return stop;
			}
			i--;
		}
		return null;
	}

}
