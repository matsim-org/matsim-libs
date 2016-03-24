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
 * contains a set of interstop routes between fromStop and toStop
 * methods:
 * getlinkweights
 * ...
 */
public class InterStopRoutes {

	private final Tuple<TransitRouteStop, TransitRouteStop> tuple;
	private final TransitRouteStop fromStop;
	private final TransitRouteStop toStop;
	private List<InterStopRoute> list = new ArrayList<>();
	private Map<Id<Link>, Double> weights = new HashMap<>();

	public InterStopRoutes(TransitRouteStop fromStop, TransitRouteStop toStop) {
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.tuple = new Tuple<>(fromStop, toStop);
	}

	public void add(InterStopRoute interStopRoute) {
		list.add(interStopRoute);
	}

	public Map<Id<Link>, Double> getLinkWeights() {
		for(InterStopRoute interStopRoute : list) {
			for(Id<Link> linkId : interStopRoute.getLinkIds()) {
				if(!weights.containsKey(linkId)) {
					weights.put(linkId, getScore(interStopRoute));
				} else {
					weights.put(linkId, weights.get(linkId) + getScore(interStopRoute));
				}
			}
		}
		return weights;
	}

	/**
	 * Calculates the score of a route based on the travel time on the links and the distance of the stop facilities
	 * from the link. Should be called after all route
	 * <br/>
	 *  score = 1.0+ weightTT*(travelTime/minTT)
	 *  + weightDistance1*(distanceStartFacilityToLink/minDist1) +
	 *  weightDistance2*(distanceEndFacilityToLink/minDist2))
	 *
	 *
	 *  <br/>Other approaches should also be tested!
	 *
	 * @return the score
	 */
	public double getScore(double weightTT, double weightDistance1, double weightDistance2) {
		return 0.0; /*weightTT*(travelTime/minTT.get(key)) +
				weightDistance1*(distanceStartFacilityToLink/minDist1.get(key)) +
				weightDistance2*(distanceEndFacilityToLink/minDist2.get(key));
				*/
	}

	/**
	 * TODO
	 * @param interStopRoute
	 * @return 1
	 */
	public double getScore(InterStopRoute interStopRoute) {
		return 1.0;
	}

	public Tuple<TransitRouteStop,TransitRouteStop> getStopsTuple() {
		return tuple;
	}

	/*
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
	 */

}