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


package playground.polettif.multiModalMap.mapping.router;

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
	private Tuple key;

	/**
	 *
	 * @param fromStop the TransitRouteStop where the subroute starts
	 * @param toStop the RouteStop where the subroute ends
	 * @param startLink the corresponding start Link
	 * @param toLink the corresponding end Link
	 * @param path between fromLink.toNode and toLink.fromNode (note: does not include start and end link, they are added on construction)
	 */
	public InterStopRoute(TransitRouteStop fromStop, TransitRouteStop toStop, Link startLink, Link toLink, LeastCostPathCalculator.Path path) {
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.key = new Tuple<>(fromStop, toStop);
		this.fromLink = startLink;
		this.toLink = toLink;

		this.distanceStartFacilityToLink = CoordUtils.calcEuclideanDistance(fromStop.getStopFacility().getCoord(), startLink.getFromNode().getCoord());
		this.distanceEndFacilityToLink = CoordUtils.calcEuclideanDistance(toStop.getStopFacility().getCoord(), toLink.getToNode().getCoord());

		if(path.links.size() == 0) {
			List<Link> linksConstr = new ArrayList<>();
			linksConstr.add(startLink);
			linksConstr.add(toLink);
			this.links = linksConstr;
			this.travelTime = 0;
		} else {
			this.links = path.links;
			this.links.add(0, startLink);
			this.links.add(toLink);
			this.travelTime = path.travelTime;
		}

		this.distanceStartFacilityToLink = CoordUtils.calcEuclideanDistance(fromStop.getStopFacility().getCoord(), startLink.getFromNode().getCoord());
		this.distanceEndFacilityToLink = CoordUtils.calcEuclideanDistance(toStop.getStopFacility().getCoord(), toLink.getToNode().getCoord());

		if(minDist1.containsKey(key)) {
			if (distanceStartFacilityToLink < minDist1.get(key))
				minDist1.put(key, distanceStartFacilityToLink);
		} else {
			minDist1.put(key, Double.MAX_VALUE);
		}

		if(minDist2.containsKey(key)) {
			if(distanceEndFacilityToLink < minDist2.get(key))
				minDist2.put(key, distanceEndFacilityToLink);
		} else {
			minDist2.put(key, Double.MAX_VALUE);
		}

		if(minTT.containsKey(key)) {
			if(travelTime < minTT.get(key))
				minTT.put(key, travelTime);
		} else {
			minTT.put(key, Double.MAX_VALUE);
		}
	}

	/**
	 * Calculates the score of a route based on the travel time on the links and the distance of the stop facilities from the link.
	 * <br/>
	 *  score = 1.0+ weightTT*(travelTime/minTT)
	 *  + weightDistance1*(distanceStartFacilityToLink/minDist1) +
	 *  weightDistance2*(distanceEndFacilityToLink/minDist2))
	 *
	 *  <br/>Other approaches should also be tested!
	 *
	 * @return the score
	 */
	public double getScore(double weightTT, double weightDistance1, double weightDistance2) {
		return 1.0+weightTT*(travelTime/minTT.get(key)) +
				weightDistance1*(distanceStartFacilityToLink/minDist1.get(key)) +
				weightDistance2*(distanceEndFacilityToLink/minDist2.get(key));
	}



	/**
	 * gets a list of link ids between the two stops, the end link is included!
	 * @return
	 */
	public List<Id<Link>> getIntermediateLinkIds() {
		List<Id<Link>> list = getLinkIds();
		list.remove(0);

		return list;
	}

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
}