/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelDisutilityUtils;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.incidents.data.TrafficItem;

/**
* @author ikaddoura
*/

public class Incident2Network {
	private static final Logger log = Logger.getLogger(Incident2Network.class);
	private final CoordinateTransformation ct;

	private Network carNetwork = null;
	private Scenario scenario = null;
	private Map<String, TrafficItem> trafficItems = null;
	
	private final Map<String, Path> trafficItemId2path = new HashMap<>();
	private final Set<String> trafficItemsToCheck = new HashSet<>();
	
	public Incident2Network(Scenario scenario, Map<String, TrafficItem> trafficItems, String targetCRS) {
		this.scenario = scenario;
		this.carNetwork = loadCarNetwork(scenario);
		this.trafficItems = trafficItems;
		ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, targetCRS);
	}

	public void computeIncidentPaths() {
		log.info("Processing traffic items...");
		
		for (String id : trafficItems.keySet()) {
						
			final Coord coordFromWGS84 = new Coord(Double.valueOf(trafficItems.get(id).getOrigin().getLongitude()), Double.valueOf(trafficItems.get(id).getOrigin().getLatitude()));
			final Coord coordToWGS84 = new Coord(Double.valueOf(trafficItems.get(id).getTo().getLongitude()), Double.valueOf(trafficItems.get(id).getTo().getLatitude()));
			
			final Coord coordFromTargetCRS = ct.transform(coordFromWGS84);
			final Coord coordToTargetCRS = ct.transform(coordToWGS84);			
			double beelineDistance = NetworkUtils.getEuclideanDistance(coordFromTargetCRS, coordToTargetCRS);
						
			// first just use the nearest link functionality
			Link nearestLinkFrom = NetworkUtils.getNearestLink(carNetwork, coordFromTargetCRS);
			Link nearestLinkTo = NetworkUtils.getNearestLink(carNetwork, coordToTargetCRS);		
			
			final DijkstraFactory f = new DijkstraFactory();
			final TravelDisutility travelCosts = TravelDisutilityUtils.createFreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
			final Path incidentPath = f.createPathCalculator(scenario.getNetwork(), travelCosts, new FreeSpeedTravelTime()).calcLeastCostPath(scenario.getNetwork().getNodes().get(nearestLinkFrom.getToNode().getId()), scenario.getNetwork().getNodes().get(nearestLinkTo.getFromNode().getId()), 0., null, null);
			
			double[] incidentVector = computeVector(coordFromTargetCRS, coordToTargetCRS);			
			
			// now cut the ends to avoid circles and other weird effects		
			
			boolean plausibleLinksIdentified = false;
			boolean implausibleLinksAtEndOfPath = false;
			
			Set<Id<Link>> linkIDsToCutOut = new HashSet<>();		

			for (Link link : incidentPath.links) {	
				
				double[] linkVector = computeVector(link.getFromNode().getCoord(), link.getToNode().getCoord());
				double scalar = computeScalarProduct(incidentVector, linkVector);
					
				if (implausibleLinksAtEndOfPath) {
					linkIDsToCutOut.add(link.getId());
					
				} else {
					if (scalar <= 0) {
						// orthogonal or more than 90 degrees
						linkIDsToCutOut.add(link.getId());
						
						if (plausibleLinksIdentified) {
							implausibleLinksAtEndOfPath = true;
						}
						
					} else {
						plausibleLinksIdentified = true;
					}
				}
			}
			
			if (linkIDsToCutOut.size() > 0) {

				if (incidentPath.links.size() > 0 && linkIDsToCutOut.size() == incidentPath.links.size()) {
					log.warn("All network links of incident " + id + " have a different direction than the incident itself. "
							+ "The inplausible paths will be written into 'incidentsLinksToBeChecked.shp'");
					this.trafficItemsToCheck.add(id);

				} else {
					
//					log.info("Cutting implausible network links from path...");
//					log.info("Previous path distance: " + computePathDistance(incidentPath));
//					log.info("Previous number of links in path: " + incidentPath.links.size());
					
					for (Id<Link> cutLinkId : linkIDsToCutOut) {
						incidentPath.links.remove(scenario.getNetwork().getLinks().get(cutLinkId));
					}

//					log.info("New path distance: " + computePathDistance(incidentPath));
//					log.info("New number of links in path: " + incidentPath.links.size());
					
				}
			}
			
			if (computePathDistance(incidentPath) > 2. * beelineDistance) {
				log.warn("No good path identified for incident " + id + ". The path distance is at least twice as long as the beeline distance."
						+ "The inplausible paths will be written into 'incidentsLinksToBeChecked.shp'. Maybe try a better network resolution.");
				this.trafficItemsToCheck.add(id);
			}
			
			if (incidentPath == null || incidentPath.links.size() == 0) {
				log.warn("No path identified for incident " + id + ".");
			}
			
			this.trafficItemId2path.put(id, incidentPath);
		}
		log.info("Processing traffic items... Done.");	
	}
	
	private double[] computeVector(Coord from, Coord to) {
		double[] vector = {to.getX() - from.getX(), to.getY() - from.getY()};
		return vector;
	}
	
	private double computeScalarProduct(double[] v1, double[] v2) {
		
		double skalarprodukt = 0;

		for (int i = 0; i < v1.length; i++) {
			skalarprodukt = skalarprodukt + v1[i] * v2[i];
		}

		return skalarprodukt;
	}

	private double computePathDistance(Path path) {
		
		double pathDistance = 0.;
		
		for (Link link : path.links) {
			pathDistance = pathDistance + link.getLength();
		}	
		
		return pathDistance;
	}
	
	private Network loadCarNetwork(Scenario scenario) {
		log.info("Creating car network... ");

		Network carNetwork = NetworkUtils.createNetwork();
		NetworkFactory factory = new NetworkFactoryImpl(carNetwork);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				
				if (!carNetwork.getNodes().containsKey(link.getFromNode().getId())) {
					carNetwork.addNode(factory.createNode(link.getFromNode().getId(), link.getFromNode().getCoord()));
				}
				if (!carNetwork.getNodes().containsKey(link.getToNode().getId())) {
					carNetwork.addNode(factory.createNode(link.getToNode().getId(), link.getToNode().getCoord()));
				}
				
				carNetwork.addLink(factory.createLink(link.getId(), link.getFromNode(), link.getToNode()));
			}
		}	
		
		log.info("Creating car network... Done.");
		return carNetwork;
	}

	public Map<String, Path> getTrafficItemId2path() {
		return trafficItemId2path;
	}

	public Set<String> getTrafficItemsToCheck() {
		return trafficItemsToCheck;
	}

}

