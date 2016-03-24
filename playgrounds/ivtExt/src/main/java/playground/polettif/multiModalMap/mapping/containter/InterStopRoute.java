/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.mapping.containter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InterStopRoute {

	private static Map<Tuple, Double> minTT = new HashMap<>();
	private static Map<Tuple, Double> minDist1 = new HashMap<>();
	private static Map<Tuple, Double> minDist2 = new HashMap<>();

	private double travelTime;
	private Link fromLink;
	private Link toLink;
	private LeastCostPathCalculator.Path path;
	private TransitRouteStop fromStop;
	private TransitRouteStop toStop;
	private double distanceStartFacilityToLink;
	private double distanceEndFacilityToLink;
	private List<Link> links;

	/**
	 *
	 * @param fromStop the TransitRouteStop where the subroute starts
	 * @param toStop the RouteStop where the subroute ends
	 * @param fromLink the corresponding start Link
	 * @param toLink the corresponding end Link
	 * @param path between fromLink.toNode and toLink.fromNode (note: does not include start and end link, they are added on construction)
	 */
	public InterStopRoute(TransitRouteStop fromStop, TransitRouteStop toStop, Link fromLink, Link toLink, LeastCostPathCalculator.Path path) {
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.fromLink = fromLink;
		this.toLink = toLink;

		if(path.links.size() == 0) {
			List<Link> linksConstr = new ArrayList<>();
			linksConstr.add(fromLink);
			linksConstr.add(toLink);
			this.links = linksConstr;
			this.travelTime = 0;
		} else {
			this.links = path.links;
			this.links.add(0, fromLink);
			this.links.add(toLink);
			this.travelTime = path.travelTime;
		}

		this.distanceStartFacilityToLink = CoordUtils.calcEuclideanDistance(fromStop.getStopFacility().getCoord(), fromLink.getFromNode().getCoord());
		this.distanceEndFacilityToLink = CoordUtils.calcEuclideanDistance(toStop.getStopFacility().getCoord(), toLink.getToNode().getCoord());
	}

	/**
	 * @return a list of link ids between the two stops, the end link is included!
	 */
	public List<Id<Link>> getIntermediateLinkIds() {
		List<Id<Link>> list = getLinkIds();
		list.remove(0);

		return list;
	}

	/**
	 * @return all links part of the path, including the first and last link
	 */
	public List<Id<Link>> getLinkIds() {
		return links.stream().map(Link::getId).collect(Collectors.toList());
	}

	/**
	 * @return the last link of a subroute
	 */
	public Link getToLink() {
		return toLink;
	}

	/**
	 * @return the first link of a subroute
	 */
	public Link getFromLink() {
		return fromLink;
	}

	public TransitRouteStop getFromStop() {
		return fromStop;
	}

	public TransitRouteStop getToStop() {
		return toStop;
	}

	public Double getTravelTime() {
		return travelTime;
	}

	public Double getScore(int i, int i1, int i2) {
		return 1.0;
	}
}