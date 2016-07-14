/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehlerScenario2Commodities
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.utils.zones;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * Maps a demand given as MATSim Population to zones that are not covering the area of the population's activity locations completely.
 * Therefore, a small network is needed that should only contain links within the area covered by zones. 
 *  
 * @author dgrether
 * 
 */
public class DgMatsimPopulation2Zones {

	private static final Logger log = Logger.getLogger(DgMatsimPopulation2Zones.class);

	private DgZones zones = null;
	
	private GeometryFactory geoFac = new GeometryFactory();

	private Network fullNetwork;

	private Network smallNetwork;

	private boolean useLinkMappings = true;

	private Map<Id, Id> originalToSimplifiedLinkIdMatching;
	
	public DgZones convert2Zones(Network network, Network smallNetwork, Map<Id, Id> originalToSimplifiedLinkIdMatching, Population pop, DgZones cells, 
			Envelope networkBoundingBox, double startTime, double endTime) {
		this.fullNetwork = network;
		this.smallNetwork = smallNetwork; 
		this.originalToSimplifiedLinkIdMatching = originalToSimplifiedLinkIdMatching;
		this.zones = cells;
		this.convertPopulation2OD(pop, networkBoundingBox, startTime, endTime);
		return cells;
	}

	private void convertPopulation2OD(Population pop, Envelope networkBoundingBox, double startTime, double endTime) {
		for (Person person : pop.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Activity startAct = null;
			Activity targetAct = null;
			Leg leg = null;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					if (startAct == null) {
						startAct = (Activity) pe;
					}
					else if (targetAct == null) {
						targetAct = (Activity) pe;
						if (startTime <= startAct.getEndTime() && startAct.getEndTime() <= endTime) {
							processLeg(startAct, leg, targetAct, networkBoundingBox);
						}
						startAct = targetAct;
						targetAct = null;
					}
				}
				else if (pe instanceof Leg) {
					leg = (Leg) pe;
				}
			}
		}
	}
	
	private void addFromZoneToZoneRelationshipToGrid(Coordinate startCoordinate, Coordinate endCoordinate){
		DgZone startCell = this.searchGridCell(startCoordinate);
		DgZone endCell = this.searchGridCell(endCoordinate);
		startCell.incrementDestinationZoneTrips(endCell);
	}

	private void addFromLinkToLinkRelationshipToGrid(Link startLink, Link endLink){
		DgZone startCell = this.searchZone4Link(startLink);
		if (this.useLinkMappings) {
			startCell.getFromLink(startLink).incrementDestinationLinkTrips(endLink);
		}
		else {
			DgZone endCell = this.searchZone4Link(endLink);
			startCell.incrementDestinationZoneTrips(endCell);
		}
	}

	private void addFromZoneToLinkRelationshipToGrid(Coordinate startCoordinate, Link endLink){
		DgZone startCell = this.searchGridCell(startCoordinate);
		if (this.useLinkMappings) {
			startCell.incrementDestinationLinkTrips(endLink);
		}
		else {
			DgZone endZone = this.searchZone4Link(endLink);
			startCell.incrementDestinationZoneTrips(endZone);
		}
	}

	private void addFromLinkToZoneRelationshipToGrid(Link startLink, Coordinate endCoordinate){
		DgZone startCell = this.searchZone4Link(startLink);
		DgZone endCell = this.searchGridCell(endCoordinate);
		if (this.useLinkMappings) {
			startCell.getFromLink(startLink).incrementDestinationZoneTrips(endCell);
		}
		else {
			startCell.incrementDestinationZoneTrips(endCell);
		}
	}

	
	private DgZone searchGridCell(Coordinate coordinate){
		Point p = this.geoFac.createPoint(coordinate);
		for (DgZone cell : this.zones.values()){
			if (cell.getPolygon().covers(p)){
				return cell;
			}
		}
		log.warn("No cell found for Coordinate: " + coordinate);
		return null;
	}
	
	private DgZone searchZone4Link(Link link) {
		Coordinate coordinate = MGC.coord2Coordinate(link.getToNode().getCoord());
		DgZone zone = this.searchGridCell(coordinate);
		if (zone == null) { // the toNode is not within the grid
			coordinate = MGC.coord2Coordinate(link.getCoord()); // try with the center of the link
			zone = this.searchGridCell(coordinate);
		}
		
		if (zone == null) { //also the center is not within the grid
			coordinate = MGC.coord2Coordinate(link.getFromNode().getCoord()); // try the from node
			zone = this.searchGridCell(coordinate);
		}
		if (zone == null) throw new IllegalStateException("No zone can be found for link id: " + link.getId() + " this should not happen.");
		return zone;
	}


	private void processLeg(Activity startAct, Leg leg, Activity targetAct,
			Envelope networkBoundingBox) {
		Coordinate startCoordinate = MGC.coord2Coordinate(startAct.getCoord());
		Coordinate endCoordinate = MGC.coord2Coordinate(targetAct.getCoord());
//		log.debug("Processing leg from: " + startCoordinate + " to " + endCoordinate);
		boolean netContainsStartCoordinate  = networkBoundingBox.contains(startCoordinate);
		boolean netContainsEndCoordinate = networkBoundingBox.contains(endCoordinate);
		if (netContainsStartCoordinate
				&& netContainsEndCoordinate) {
//			log.debug("  coordinates in grid...");
			this.addFromZoneToZoneRelationshipToGrid(startCoordinate, endCoordinate);
		}
		else if (netContainsStartCoordinate && ! netContainsEndCoordinate){ // zone 2 link
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Link> route = this.createFullRoute(networkRoute);
			Link lastLink = null;
			Coordinate coordinate = null;
			for (int i = route.size() - 1; i >= 0; i--){
				Link link = route.get(i);
				Id linkId = link.getId();
				if (this.originalToSimplifiedLinkIdMatching.containsKey(linkId)) {
					linkId = this.originalToSimplifiedLinkIdMatching.get(linkId);
					link = smallNetwork.getLinks().get(linkId);
				}
				if (this.smallNetwork.getLinks().containsKey(link.getId())){
					lastLink = link;
					break;
				}
//				coordinate = MGC.coord2Coordinate(link.getCoord());
//				if (! networkBoundingBox.contains(coordinate)) {
//					break;
//				}
			}
			if (lastLink != null) {
				this.addFromZoneToLinkRelationshipToGrid(startCoordinate, lastLink);
			}
		}
		else if (! netContainsStartCoordinate &&  netContainsEndCoordinate){ // link 2 zone
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Link> route = this.createFullRoute(networkRoute);
			Link firstLink = null;
			Coordinate coordinate = null;
			for (Link link : route){
				Id linkId = link.getId();
				if (this.originalToSimplifiedLinkIdMatching.containsKey(linkId)) {
					linkId = this.originalToSimplifiedLinkIdMatching.get(linkId);
					link = smallNetwork.getLinks().get(linkId);
				}
				if (this.smallNetwork.getLinks().containsKey(link.getId())){
					firstLink = link;
					break;
				}
//				coordinate = MGC.coord2Coordinate(link.getCoord());
//				if (networkBoundingBox.contains(coordinate)) {
//					firstLink = link;
//					break;
//				}
			}
			if (firstLink != null){
				this.addFromLinkToZoneRelationshipToGrid(firstLink, endCoordinate);
			}
		}
		else { // link 2 link
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Link> route = this.createFullRoute(networkRoute);
//			List<Coordinate> coordinateSequence = this.createCoordinateSequenceFromRoute(network, networkRoute);
			boolean isRouteInGrid = false;
			while (! route.isEmpty()){
				Tuple<Link, Link> nextFromTo = this.getNextFromToOfRoute(route, networkBoundingBox);
				if (nextFromTo != null){
					this.addFromLinkToLinkRelationshipToGrid(nextFromTo.getFirst(), nextFromTo.getSecond());
					isRouteInGrid = true;
				}
			}
//			if (! isRouteInGrid){
//				log.debug("  Route is not in area of interest");
//			}
		}
	}

	private List<Link> createFullRoute(NetworkRoute route) {
		List<Id> linkIds = new ArrayList<Id>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		List<Link> links = new ArrayList<Link>();
		for (Id linkId : linkIds){
			Link currentLink = this.fullNetwork.getLinks().get(linkId);
			links.add(currentLink);
		}
		return links;
	}

	
	private Tuple<Link, Link> getNextFromToOfRoute(List<Link> route,
			Envelope networkBoundingBox) {
		Link routeStartLink = null;
		Link routeEndLink = null;
		Link currentLink = null;
//		Coordinate currentCoordinate = null;
		while (! route.isEmpty()){
			currentLink = route.remove(0);
			Id linkId = currentLink.getId();
			if (this.originalToSimplifiedLinkIdMatching.containsKey(linkId)) {
				linkId = this.originalToSimplifiedLinkIdMatching.get(linkId);
				currentLink = smallNetwork.getLinks().get(linkId);
			}
			if (this.smallNetwork.getLinks().containsKey(currentLink.getId())){
				routeStartLink = currentLink;
				break;
			}
//			currentCoordinate = MGC.coord2Coordinate(currentLink.getCoord());
//			if (networkBoundingBox.contains(currentCoordinate)){
//				routeStartLink = currentLink;
//				break;
//			}
		}
		//search last link that is contained in grid
		while (! route.isEmpty()){
			currentLink = route.remove(0);
			Id linkId = currentLink.getId();
			if (this.originalToSimplifiedLinkIdMatching.containsKey(linkId)) {
				linkId = this.originalToSimplifiedLinkIdMatching.get(linkId);
				currentLink = smallNetwork.getLinks().get(linkId);
				if (currentLink == null) throw new IllegalStateException("Link Id " + linkId  + " not found in small network");
			}
			if (this.smallNetwork.getLinks().containsKey(currentLink.getId())){
				routeEndLink = currentLink;
			}
//			currentCoordinate = MGC.coord2Coordinate(currentLink.getCoord());
//			if (networkBoundingBox.contains(currentCoordinate)){
//				routeEndLink = currentLink;
//			}
			else {
				break;
			}
		}
		if (routeStartLink != null && routeEndLink != null){
			return new Tuple<Link, Link>(routeStartLink, routeEndLink);
		}
		return null;
	}
	
	/**
	 * 
	 */
	public void setUseLinkMappings(boolean b) {
		this.useLinkMappings = b;
	}
	
}
