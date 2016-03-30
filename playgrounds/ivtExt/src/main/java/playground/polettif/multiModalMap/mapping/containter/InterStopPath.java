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

import java.util.*;
import java.util.stream.Collectors;

public class InterStopPath {

	private final Tuple<Link, Link> id;
	private final TransitRouteStop fromStop;
	private final TransitRouteStop toStop;
	private final Link fromLink;
	private final Link toLink;
	private final List<Link> pathLinks;

	private double travelTime;
	private LeastCostPathCalculator.Path path;
	private double distanceStartFacilityToLink;
	private double distanceEndFacilityToLink;

	/**
	 *
	 * @param fromStop the TransitRouteStop where the subroute starts
	 * @param toStop the RouteStop where the subroute ends
	 * @param fromLink the corresponding start Link
	 * @param toLink the corresponding end Link
	 * @param path between fromLink.toNode and toLink.fromNode (note: does not include start and end link, they are added on construction)
	 */
	public InterStopPath(TransitRouteStop fromStop, TransitRouteStop toStop, Link fromLink, Link toLink, LeastCostPathCalculator.Path path) {
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.fromLink = fromLink;
		this.toLink = toLink;
		this.id = new Tuple<>(fromLink, toLink);

		if(path.links.size() == 0) {
			// links are immediately next to each other
			this.pathLinks = null;
			this.travelTime = 0;
		} else {
			this.pathLinks = path.links;
			this.travelTime = path.travelTime;
		}

		this.distanceStartFacilityToLink = CoordUtils.calcEuclideanDistance(fromStop.getStopFacility().getCoord(), fromLink.getFromNode().getCoord());
		this.distanceEndFacilityToLink = CoordUtils.calcEuclideanDistance(toStop.getStopFacility().getCoord(), toLink.getToNode().getCoord());
	}

	/**
	 * @return all pathLinks part of the path, including the first and last link
	 */
	public List<Id<Link>> getAllLinkIds() {
		List<Id<Link>> list = new ArrayList<>();
		list.add(fromLink.getId());
		if(pathLinks != null) {
			list.addAll(getIntermediateLinkIds());
		}
		list.add(toLink.getId());

		return list;
	}

	/**
	 * @return a list of link ids between the two stops
	 */
	public List<Id<Link>> getIntermediateLinkIds() {
		if(pathLinks != null)
			return pathLinks.stream().map(Link::getId).collect(Collectors.toList());
		else
			return null;
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

	public Double getDistanceStartFacilityToLink() {
		return distanceStartFacilityToLink;
	}

	public Double getdistanceEndFacilityToLink() {
		return distanceEndFacilityToLink;
	}

	public Tuple<Link, Link> getId() {
		return id;
	}

	public static List<Id<Link>> getLinkIdsFromPath(LeastCostPathCalculator.Path path) {
		return path.links.stream().map(Link::getId).collect(Collectors.toList());

	}

	@Deprecated
	public Double getScore(int i, int i1, int i2) {
		return 1.0;
	}
}