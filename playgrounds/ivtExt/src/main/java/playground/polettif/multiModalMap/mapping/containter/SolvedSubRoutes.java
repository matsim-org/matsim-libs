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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

public class SolvedSubRoutes implements SubRoutes {

	Map<Tuple, InterStopRoute> subroutes = new HashMap<>();
	Map<TransitStopFacility, Id<Link>> getStopFacilityRefLinkIds = new HashMap<>();

	public SolvedSubRoutes() {
	}

	public InterStopRoute getInterStopRoute(TransitRouteStop previousStop, TransitRouteStop currentStop) {
		return subroutes.get(new Tuple<>(previousStop, currentStop));
	}

	public void put(InterStopRoute interStopRoute) {
		subroutes.put(new Tuple<>(interStopRoute.getFromStop(), interStopRoute.getToStop()), interStopRoute);
	}

	public List<Id<Link>> getLinkIdList(List<TransitRouteStop> stopSequence) {
		List<Id<Link>> list = new ArrayList<>();

		int i = 0;
		while(i < stopSequence.size()-1) {
			InterStopRoute interStopRoute = subroutes.get(new Tuple<>(stopSequence.get(i), stopSequence.get(i+1)));
			list.addAll(interStopRoute.getIntermediateLinkIds());
			i++;
		}

		// add very first link
		list.add(0, subroutes.get(new Tuple<>(stopSequence.get(0), stopSequence.get(1))).getFromLink().getId());

		return list;
	}

	@Override
	public boolean contains(TransitRouteStop fromStop, TransitRouteStop toStop) {
		return subroutes.containsKey(new Tuple<>(fromStop, toStop));
	}

	public Map<TransitStopFacility, Id<Link>> getStopFacilityRefLinkIds() {
		return getStopFacilityRefLinkIds;
	}


}