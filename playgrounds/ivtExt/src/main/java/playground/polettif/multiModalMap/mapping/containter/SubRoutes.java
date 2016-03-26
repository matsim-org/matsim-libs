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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container to store all interStopRoutes of a schedule/network
 *
 * 	AllInterStopRoutes [1-n] InterStopPathSet [1-n] InterStopPath
 *
 * 	access interStopPathSet via [fromStop, toStop]
 * 	access interStopPath via [fromLink, toLink]
 *
 */
public class SubRoutes {

	private Map<Tuple<TransitRouteStop, TransitRouteStop>, InterStopPathSet> subRoutes = new HashMap<>();

	public SubRoutes() {
	}

	public boolean contains(TransitRouteStop fromStop, TransitRouteStop toStop) {
		return subRoutes.containsKey(new Tuple<>(fromStop, toStop));
	}

	public void add(InterStopPathSet interStopPaths) {
		subRoutes.put(interStopPaths.getId(), interStopPaths);
	}

	public InterStopPathSet get(TransitRouteStop currentStop, TransitRouteStop nextStop) {
		return subRoutes.get(new Tuple<>(currentStop, nextStop));
	}

	/**
	 * Each interStopRoute that passes a link adds weight to the link. Higher weight means more paths have
	 * passed a link. The weight is calculated in {@link #getWeight(InterStopPath)} and based on the travelTime of the path.
	 *
	 * @return the weights
	 */
	public Map<Id<Link>, Double> getTransitRouteLinkWeights(List<TransitRouteStop> routeStops) {

		Map<Id<Link>, Double> weights = new HashMap<>();

		List<InterStopPath> list = new ArrayList<>();
		for(int i = 1; i<routeStops.size(); i++) {
			TransitRouteStop currentStop = routeStops.get(i);
			TransitRouteStop previousStop = routeStops.get(i-1);

			InterStopPathSet interStopPathSet = this.get(previousStop, currentStop);
			list.addAll(interStopPathSet.getPaths());
		}

		for (InterStopPath interStopPath : list) {
			for (Id<Link> linkId : interStopPath.getAllLinkIds()) {
				if (!weights.containsKey(linkId)) {
					weights.put(linkId, getWeight(interStopPath));
				} else {
					weights.put(linkId, weights.get(linkId) + getWeight(interStopPath));
				}
			}
		}
		return weights;
	}

	/**
	 * returns the score assigned to all links of a route.<br/>
	 * score = 3*3600-travelTime<br/>
	 *
	 * @param interStopPath
	 * @return 1
	 */
	public double getWeight(InterStopPath interStopPath) {
		return 2*3600-interStopPath.getTravelTime();
	}


}