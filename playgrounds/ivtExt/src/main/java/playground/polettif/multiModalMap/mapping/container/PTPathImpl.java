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

package playground.polettif.multiModalMap.mapping.container;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Transit;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;
import java.util.stream.Collectors;

@Deprecated
public class PTPathImpl implements PTPath {

	private final Tuple<TransitStopFacility, TransitStopFacility> stopPair;
	private final TransitStopFacility fromStopFacility;
	private final TransitStopFacility toStopFacility;
	private final Link fromLink;
	private final Link toLink;
	private final double travelTime;
	private final double interTravelTime;

	private List<Link> links;
	private List<Link> intermediateLinks;
	private double distanceStartFacilityToLink;
	private double distanceEndFacilityToLink;
	private TransitStopFacility viaStopFacility;
	private Link viaLink = null;


	/**
	 *  @param fromStopFacility the TransitRouteStop where the subroute starts
	 * @param fromLink the corresponding start Link
	 * @param toStopFacility the RouteStop where the subroute ends
	 * @param toLink the corresponding end Link
	 * @param path between fromLink.toNode and toLink.fromNode (note: does not include start and end link, they are added on construction)
	 */
	public PTPathImpl(TransitStopFacility fromStopFacility, Link fromLink, TransitStopFacility toStopFacility, Link toLink, LeastCostPathCalculator.Path path) {
		this.fromStopFacility = fromStopFacility;
		this.toStopFacility = toStopFacility;
		this.stopPair = new Tuple<>(fromStopFacility, toStopFacility);

		this.fromLink = fromLink;
		this.toLink = toLink;

		this.intermediateLinks = path.links;

		this.links = new ArrayList<>();
		this.links.add(fromLink);
		this.links.addAll(path.links);
		this.links.add(toLink);

		this.distanceStartFacilityToLink = CoordUtils.calcEuclideanDistance(fromStopFacility.getCoord(), fromLink.getFromNode().getCoord());
		this.distanceEndFacilityToLink = CoordUtils.calcEuclideanDistance(toStopFacility.getCoord(), toLink.getToNode().getCoord());

		this.travelTime = getLinkTravelTime(fromLink) + path.travelTime + getLinkTravelTime(toLink);
		this.interTravelTime = getLinkTravelTime(fromLink) + path.travelTime + getLinkTravelTime(toLink);
	}

	private double getLinkTravelTime(Link link) {
		return link.getLength()/link.getFreespeed();
	}


	@Override
	public List<Id<Link>> getLinkIds() {
		return links.stream().map(Link::getId).collect(Collectors.toList());
	}

	@Override
	public List<Id<Link>> getIntermediateLinkIds() {
		return intermediateLinks.stream().map(Link::getId).collect(Collectors.toList());
	}

	@Override
	public List<Id<Link>> getLinkIdsExcludingToLink() {
		List<Id<Link>> list = new ArrayList<>();
		list.add(fromLink.getId());
		list.addAll(getIntermediateLinkIds());

		return list;
	}

	@Override
	public List<Id<Link>> getLinkIdsExcludingFromLink() {
		List<Id<Link>> list = new ArrayList<>();
		list.addAll(getIntermediateLinkIds());
		list.add(toLink.getId());

		return list;
	}

	/**
	 * @return the last link of a subroute
	 */
	@Override
	public Link getToLink() {
		return toLink;
	}


	@Override
	public Link getFromLink() {
		return fromLink;
	}

	@Override
	public TransitStopFacility getFromStopFacility() {
		return fromStopFacility;
	}

	@Override
	public TransitStopFacility getToStopFacility() {
		return toStopFacility;
	}

	@Override
	public double getTravelTime() {
		return travelTime;
	}

	@Override
	public double getDistanceStartFacilityToLink() {
		return distanceStartFacilityToLink;
	}

	@Override
	public double getDistanceEndFacilityToLink() {
		return distanceEndFacilityToLink;
	}

	@Override
	public Tuple<TransitStopFacility, TransitStopFacility> getStopPair() {
		return stopPair;
	}

	@Override
	public void addViaLink(Link link) {
		this.viaLink = link;
	}

	@Override
	public List<Id<Link>> getLinkCandidateIds() {
		ArrayList<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(fromLink.getId());
		if(viaLink != null) { linkIds.add(viaLink.getId()); }
		linkIds.add(toLink.getId());

		return linkIds;
	}

	@Override
	public double getInterTravelTime() {
		return interTravelTime;
	}
}