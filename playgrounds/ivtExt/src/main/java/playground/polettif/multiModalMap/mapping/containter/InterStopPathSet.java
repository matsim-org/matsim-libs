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
 * contains a set of interstop paths between fromStop and toStop
 */
public class InterStopPathSet {

	private final Tuple<TransitRouteStop, TransitRouteStop> id;
	private final TransitRouteStop fromStop;
	private final TransitRouteStop toStop;

	private Map<Tuple<Link, Link>, InterStopPath> interStopPaths = new HashMap<>();
	private Map<Id<Link>, Double> weights = null;
	private double minDist1 = Double.MAX_VALUE;
	private double minDist2 = Double.MAX_VALUE;
	private double minTT = Double.MAX_VALUE;

	public InterStopPathSet(TransitRouteStop fromStop, TransitRouteStop toStop) {
		this.fromStop = fromStop;
		this.toStop = toStop;
		this.id = new Tuple<>(fromStop, toStop);
	}

	public void add(InterStopPath interStopPath) {
		interStopPaths.put(new Tuple<>(interStopPath.getFromLink(), interStopPath.getToLink()), interStopPath);
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
	@Deprecated
	public double getScore(double weightTT, double weightDistance1, double weightDistance2) {
		return 0.0; /*weightTT*(travelTime/minTT.get(key)) +
				weightDistance1*(distanceStartFacilityToLink/minDist1.get(key)) +
				weightDistance2*(distanceEndFacilityToLink/minDist2.get(key));
				*/
	}


	public Tuple<TransitRouteStop,TransitRouteStop> getId() {
		return id;
	}

	private void calculateMinimalValues() {
		if (minTT == Double.MAX_VALUE) {
			for (InterStopPath isr : interStopPaths.values()) {
				if (isr.getDistanceStartFacilityToLink() < minDist1)
					minDist1 = isr.getDistanceStartFacilityToLink();

				if (isr.getdistanceEndFacilityToLink() < minDist2)
					minDist2 = isr.getDistanceStartFacilityToLink();

				if (isr.getTravelTime() < minTT)
					minTT = isr.getDistanceStartFacilityToLink();
			}
		}
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
}