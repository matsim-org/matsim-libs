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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * contains a set of interstop paths between fromStop and toStop
 */
public class InterStopPathSet {

	private final Tuple<TransitRouteStop, TransitRouteStop> id;
	private final TransitRouteStop fromStop;
	private final TransitRouteStop toStop;

	private Map<Tuple<Link, Link>, InterStopPath> interStopPaths = new HashMap<>();

	/**
	 * minimal distance between the stop facility and the link for the fromStop
	 */
	private double minDistFrom = Double.MAX_VALUE;
	/**
	 * minimal distance between the stop facility and the link for the toStop
	 */
	private double minDistTo = Double.MAX_VALUE;
	/**
	 * minimal travel time betweent the two stops
	 */
	private double minTT = Double.MAX_VALUE;

	public InterStopPathSet(TransitRouteStop fromStop, TransitRouteStop toStop) {
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.id = new Tuple<>(fromStop, toStop);
	}

	public void put(InterStopPath interStopPath) {
		interStopPaths.put(new Tuple<>(interStopPath.getFromLink(), interStopPath.getToLink()), interStopPath);

		if(interStopPath.getTravelTime() < minTT)
			minTT = interStopPath.getTravelTime();

		if(interStopPath.getdistanceEndFacilityToLink() < minDistFrom)
			minDistFrom = interStopPath.getDistanceStartFacilityToLink();

		if(interStopPath.getdistanceEndFacilityToLink() < minDistTo)
			minDistTo = interStopPath.getDistanceStartFacilityToLink();
	}


	public Tuple<TransitRouteStop,TransitRouteStop> getId() {
		return id;
	}

	public List<InterStopPath> getPaths() {
		return new ArrayList<>(interStopPaths.values());
	}

	public InterStopPath getPath(Link currentLink, Link nextLink) {
		return interStopPaths.get(new Tuple<>(currentLink, nextLink));
	}

	public boolean contains(Link fromLink, Link toLink) {
		return interStopPaths.containsKey(new Tuple<>(fromLink, toLink));
	}

	public List<Id<Link>> getAllIntermediateLinkIds() {
		List<Id<Link>> allIntermediateLinkIds = new ArrayList<>();

		for(InterStopPath isp : interStopPaths.values()) {
			allIntermediateLinkIds.addAll(isp.getIntermediateLinkIds());
		}

		return allIntermediateLinkIds;
	}
}