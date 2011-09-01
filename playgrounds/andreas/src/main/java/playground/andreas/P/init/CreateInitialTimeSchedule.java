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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.NetworkUtils;
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
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.vehicles.VehiclesImpl;

@Deprecated
public class CreateInitialTimeSchedule {
	
	private NetworkImpl net;
	private Vehicles veh;
	private TransitSchedule tS;
	private int numberOfAgents;
	
	public static void createInitialTimeSchedule(PConfigGroup pConfig){
		Coord minXY = new CoordImpl(pConfig.getMinX(), pConfig.getMinY());
		Coord maxXY = new CoordImpl(pConfig.getMaxX(), pConfig.getMaxY());
		
		CreateInitialTimeSchedule cITS = new CreateInitialTimeSchedule(pConfig.getNetwork(), pConfig.getGridDistance(), minXY, maxXY, pConfig.getNumberOfAgents());
		cITS.run();
		cITS.writeTransitSchedule(pConfig.getCurrentOutputBase() + "transitSchedule.xml");
		cITS.writeVehicles(pConfig.getCurrentOutputBase() + "transitVehicles.xml");
	}
	
	public static TransitLine createSingleTransitLine(NetworkImpl net, TransitSchedule transitSchedule, Id driverId){
		CreateInitialTimeSchedule cITS = new CreateInitialTimeSchedule(net, transitSchedule);
		return cITS.createSingleTransitLine(driverId);
	}

	public CreateInitialTimeSchedule(String networkInFile, double gridDistance, Coord minXY, Coord maxXY, int numberOfAgents) {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(sc);
		netReader.readFile(networkInFile);		
		this.net = sc.getNetwork();
		
		this.tS = CreateStops.createStops(this.net, gridDistance, minXY, maxXY);
		this.veh = new VehiclesImpl();
		this.numberOfAgents = numberOfAgents;
	}

	public CreateInitialTimeSchedule(NetworkImpl net, TransitSchedule transitSchedule) {
		this.net = net;
		this.tS = transitSchedule;
	}

	private void run() {
		for (int i = 0; i < this.numberOfAgents; i++) {
			
			// initialize
			Id driverId = new IdImpl("free_" + i);
			this.tS.addTransitLine(createSingleTransitLine(driverId));
		}
	}

	public TransitLine createSingleTransitLine(Id driverId){
		// initialize
		TransitLine line = this.tS.getFactory().createTransitLine(driverId);			
		TransitStopFacility startStop = getRandomTransitStop();			
		TransitStopFacility endStop = getRandomTransitStop();
		
		TransitRoute transitRoute_H = createRoute(new IdImpl(driverId + "_H"), startStop, endStop);
		TransitRoute transitRoute_R = createRoute(new IdImpl(driverId + "_R"), endStop, startStop);
		
		// register route
		line.addRoute(transitRoute_H);
		line.addRoute(transitRoute_R);
		
		// add departures
		int n = 0;
		for (double j = 0.0; j < 24 * 3600; ) {
			Departure departure = this.tS.getFactory().createDeparture(new IdImpl(n), j);
			departure.setVehicleId(driverId);
			transitRoute_H.addDeparture(departure);
			j += transitRoute_H.getStop(endStop).getDepartureOffset() + 5 *60;
			n++;
			
			departure = this.tS.getFactory().createDeparture(new IdImpl(n), j);
			departure.setVehicleId(driverId);
			transitRoute_R.addDeparture(departure);
			j += transitRoute_R.getStop(startStop).getDepartureOffset() + 5 *60;
			n++;
		}
		
		return line;
	}

	private TransitRoute createRoute(Id routeID, TransitStopFacility startStop, TransitStopFacility endStop){
		
		FreespeedTravelTimeCost tC = new FreespeedTravelTimeCost(-6.0, 0.0, 0.0);
		LeastCostPathCalculator routingAlgo = new Dijkstra(this.net, tC, tC);
		
		Node startNode = this.net.getLinks().get(startStop.getLinkId()).getToNode();
		Node endNode = this.net.getLinks().get(endStop.getLinkId()).getFromNode();
		
		int startTime = 0 * 3600;
		
		// get Route
		Path path = routingAlgo.calcLeastCostPath(startNode, endNode, startTime);
		NetworkRoute route = (NetworkRoute) this.net.getFactory().createRoute(TransportMode.car, startStop.getLinkId(), endStop.getLinkId());
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
		VehicleType vehType = vehFactory.createVehicleType(new IdImpl("defaultTransitVehicleType"));
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(8));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehType.setCapacity(capacity);
		this.veh.getVehicleTypes().put(vehType.getId(), vehType);
	
		for (TransitLine line : this.tS.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicle = vehFactory.createVehicle(departure.getVehicleId(), vehType);
					this.veh.getVehicles().put(vehicle.getId(), vehicle);
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
