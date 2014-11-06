/* *********************************************************************** *
 * project: org.matsim.*
 * DgAirNetworkBuilder
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import air.scenario.oag.DgOagFlight;
import air.scenario.oag.DgOagFlightsData;

/**
 * @author dgrether
 * 
 */
public class DgAirNetworkBuilder {
	
	private static final Logger log = Logger.getLogger(DgAirNetworkBuilder.class);
	
	private static final double MACH_2 = 686.0;
	private static final double CAP_PERIOD = 3600.0;

	private Scenario scenario;

	private Map<Id<Node>, SfMatsimAirport> airportMap;

	private CoordinateTransformation transform;

	private DgFlightScenarioData modelConfig;

	public DgAirNetworkBuilder(Scenario scenario, CoordinateTransformation transform, DgFlightScenarioData modelConfig) {
		this.scenario = scenario;
		this.transform = transform;
		this.modelConfig = modelConfig;
	}

	private Map<Id<Node>, SfMatsimAirport> createAndAddAirports(Map<String, Coord> airports, Network network){
		Map<Id<Node>, SfMatsimAirport> airportMap = new HashMap<>();
		for (Entry<String, Coord> e : airports.entrySet()) {
			Coord transformedCoord = this.transform.transform(e.getValue());
			DgAirportCapacity capacityData = modelConfig.getAirportsCapacityData().getAirportCapacity(e.getKey());
			SfMatsimAirport airport = new SfMatsimAirport(Id.create(e.getKey(), Node.class), transformedCoord, capacityData);
			airportMap.put(airport.getId(), airport);
			if (DgCreateSfFlightScenario.NUMBER_OF_RUNWAYS == 2) {
				airport.createTwoRunways(network);
			}
			else {
				airport.createOneRunway(network);
			}
		}
		return airportMap;
	}
	
	private void createAndAddConnections(DgOagFlightsData flightsData, Map<Id<Node>, SfMatsimAirport> airportMap, NetworkImpl network){
		Set<String> allowedModes = new HashSet<String>();
		allowedModes.add("pt");
		allowedModes.add("car");

		for (DgOagFlight flight : flightsData.getFlightDesignatorFlightMap().values()){
			SfMatsimAirport oa = airportMap.get(Id.create( flight.getOriginCode(), Node.class));
			SfMatsimAirport da = airportMap.get(Id.create( flight.getDestinationCode(), Node.class));
			
			Node startNode = network.getNodes().get(oa.getOutgoingFlightsNodeId());
			Node endNode = network.getNodes().get(da.getIncomingFlightsNodeId());
			
			Id<Link> linkId = Id.create(flight.getOriginCode() + "_" + flight.getDestinationCode() + "_" + flight.getFlightDesignator(), Link.class);
			Link originToDestination = network.getFactory().createLink(linkId, startNode, endNode);
			
			originToDestination.setAllowedModes(allowedModes);
			originToDestination.setCapacity(1.0*CAP_PERIOD);
			originToDestination.setLength(flight.getDistanceKm()  * 1000.0);
			
			double speed = flight.getDistanceKm() * 1000.0 / ( flight.getScheduledDuration() - oa.getTaxiTimeOutbound() - da.getTaxiTimeInbound());
			originToDestination.setFreespeed(speed);
			if (! network.getLinks().containsKey(linkId)) {
				network.addLink(originToDestination);
			}
		}
		

	}
	
	
	public Network createNetwork(DgOagFlightsData flightsData, Map<String, Coord> airports,
			String outputNetworkFilename) {
		NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
		network.setCapacityPeriod(CAP_PERIOD);
		
		this.airportMap = this.createAndAddAirports(airports, network);

		
		this.createAndAddConnections(flightsData, airportMap, network);
		
		new NetworkWriter(network).write(outputNetworkFilename);
		
		log.info("Done! Unprocessed MATSim Network saved as " + outputNetworkFilename);
		
		log.info("Anzahl Flughäfen: "+ airportMap.size());
		log.info("Anzahl Links: "+ network.getLinks().size());
		log.info("Anzahl Verbindungen: " + (network.getLinks().size() - (airportMap.size() * (DgCreateSfFlightScenario.NUMBER_OF_RUNWAYS + 3))) );
		return network;
	}
	
	public Map<Id<Node>, SfMatsimAirport>  getAirportMap(){
		return this.airportMap;
	}

}
