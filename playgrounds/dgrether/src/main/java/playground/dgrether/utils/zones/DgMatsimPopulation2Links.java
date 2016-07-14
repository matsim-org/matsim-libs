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
 * @author tthunig
 * 
 */
public class DgMatsimPopulation2Links {

	private static final Logger log = Logger.getLogger(DgMatsimPopulation2Links.class);

	private DgZones zones = null;
	
	private GeometryFactory geoFac = new GeometryFactory();

	private Network fullNetwork;

	private Network smallNetwork;

	private Map<Id<Link>, Id<Link>> originalToSimplifiedLinkIdMatching;
	
	public DgZones convert2Links(Network network, Network smallNetwork, Map<Id<Link>, Id<Link>> originalToSimplifiedLinkIdMatching, Population pop, DgZones cells, 
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
	
	private void addFromLinkToLinkRelationshipToGrid(Link startLink, Link endLink){
		DgZone startCell = this.searchZone4Link(startLink);
		startCell.getFromLink(startLink).incrementDestinationLinkTrips(endLink);
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
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			List<Link> route = this.createFullRoute(networkRoute);
			// create an od-pair between the first and the last link on the route which is in the small network
			// no matter whether there are links between them which are not in the small network
			if (! route.isEmpty()){
				Tuple<Link, Link> nextFromTo = this.getNextFromToOnSmallNetworkOfRoute(route);
				if (nextFromTo != null){
					this.addFromLinkToLinkRelationshipToGrid(nextFromTo.getFirst(), nextFromTo.getSecond());
				}
			}
	}

	private List<Link> createFullRoute(NetworkRoute route) {
		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		List<Link> links = new ArrayList<Link>();
		for (Id<Link> linkId : linkIds) {
			Link currentLink = this.fullNetwork.getLinks().get(linkId);
			if (currentLink == null) {
				if (linkId.equals(Id.create("5635", Link.class))){
					currentLink = this.fullNetwork.getLinks().get(Id.create("5892", Link.class));
				}
				else {
					log.error("Network does not contain link id " + linkId);
				}
			}
			links.add(currentLink);
		}
		return links;
	}

	
	private Tuple<Link, Link> getNextFromToOnSmallNetworkOfRoute(List<Link> route) {
		Link routeStartLink = null;
		Link routeEndLink = null;
		Link currentLink = null;
		//search first link that is in the small network
		while (! route.isEmpty()){
			currentLink = route.remove(0);
			Id<Link> linkId = currentLink.getId();
			if (this.originalToSimplifiedLinkIdMatching.containsKey(linkId)) {
				linkId = this.originalToSimplifiedLinkIdMatching.get(linkId);
				currentLink = smallNetwork.getLinks().get(linkId);
			}
			if (this.smallNetwork.getLinks().containsKey(currentLink.getId())){
				routeStartLink = currentLink;
				break;
			}
		}
		//search last link that is in the small network
		while (! route.isEmpty()){
			currentLink = route.remove(route.size()-1);
			Id<Link> linkId = currentLink.getId();
			if (this.originalToSimplifiedLinkIdMatching.containsKey(linkId)) {
				linkId = this.originalToSimplifiedLinkIdMatching.get(linkId);
				currentLink = smallNetwork.getLinks().get(linkId);
				if (currentLink == null) throw new IllegalStateException("Link Id " + linkId  + " not found in small network");
			}
			if (this.smallNetwork.getLinks().containsKey(currentLink.getId())){
				routeEndLink = currentLink;
				break;
			}
		}
		if (routeStartLink != null && routeEndLink != null){
			return new Tuple<Link, Link>(routeStartLink, routeEndLink);
		}
		return null;
	}
	
	
}
