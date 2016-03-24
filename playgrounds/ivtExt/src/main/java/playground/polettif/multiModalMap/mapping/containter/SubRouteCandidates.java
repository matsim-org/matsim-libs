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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubRouteCandidates implements SubRoutes {

	private Map<Tuple, List<InterStopRoute>> allSubRoutes = new HashMap<>();
	private Map<Tuple, Map<Id<Link>, Integer>> allLinkWeights = new HashMap<>();

	public SubRouteCandidates() {

	}

	public Map<Id<Link>, Integer> getLinkWeightList(List<TransitRouteStop> routeStops) {

		for(TransitRouteStop stop : routeStops) {

		}

		Tuple key = null; //new Tuple<>(fromStop, toStop);

		if(allLinkWeights.containsKey(key)) {
			return allLinkWeights.get(key);
		} else {
			Map<Id<Link>, Integer> linkWeights = new HashMap<>();
			for(InterStopRoute interStopRoute : allSubRoutes.get(key)) {
				for(Id<Link> linkId : interStopRoute.getLinkIds()) {
					int count = linkWeights.containsKey(linkId) ? linkWeights.get(linkId) : 0;
					linkWeights.put(linkId, count + 1);
				}
			}
			allLinkWeights.put(key, linkWeights);
			return linkWeights;
		}
	}

	@Override
	public boolean contains(TransitRouteStop fromStop, TransitRouteStop toStop) {
		return allSubRoutes.containsKey(new Tuple<>(fromStop, toStop));
	}
}