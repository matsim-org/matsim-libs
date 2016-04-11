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


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkWeightCalculator {

	protected static Logger log = Logger.getLogger(LinkWeightCalculator.class);


	private final List<TransitRouteStop> routeStops;
	private final TransitStopFacility firstStopFacility;
	private final TransitStopFacility lastStopFacility;
	private Map<TransitStopFacility, PTPath> ptPaths = new HashMap<>();
	private Map<Id<Link>, Double> weights = new HashMap<>();
	private double maxTT;
	private Map<Tuple<TransitStopFacility, TransitStopFacility>, Double> minTTs = new HashMap<>();

	public LinkWeightCalculator(List<TransitRouteStop> routeStops) {
		this.routeStops = routeStops;
		firstStopFacility = routeStops.get(0).getStopFacility();
		lastStopFacility = routeStops.get(routeStops.size()-1).getStopFacility();
	}

	public void add(PTPath ptPath) {
		ptPaths.put(ptPath.getFromStopFacility(), ptPath);

		if(ptPath.getTravelTime() < maxTT) {
			maxTT = ptPath.getTravelTime();
		}

		if(ptPath.getTravelTime() < MapUtils.getDouble(ptPath.getStopPair(), minTTs, Double.MAX_VALUE)) {
			minTTs.put(ptPath.getStopPair(), ptPath.getTravelTime());
		}
	}

	public Map<Id<Link>, Double> getLinkWeights() {

		for(PTPath ptPath : ptPaths.values()) {

			double pathWeight = getLinkWeight(ptPath, 1800);

			/**
			 * Links that have been fixed on calculating paths are excluded from link weight calculations, except
			 * for the link candidates of the first and last stop
			 */
			for (Id<Link> linkId : ptPath.getLinkCandidateIds()) {
				MapUtils.addToDouble(linkId, weights, pathWeight, pathWeight);
			}
		}

		return weights;
	}

	@Deprecated
	public Map<Id<Link>, Double> getLinkWeightsMinTTrelative() {

		for(PTPath ptPath : ptPaths.values()) {

			int nStops = getNumberOfStopsInBetween(ptPath);

			double minTT = minTTs.get(ptPath.getStopPair());

			double pathWeight = 100*ptPath.getTravelTime()/minTT;

			for(Id<Link> linkId : ptPath.getLinkIds()) {
				MapUtils.addToDouble(linkId, weights, pathWeight, pathWeight);
			}
		}

		return weights;
	}

	/**
	 * Can be used for link weight calculations.
	 * @param ptPath
	 * @return the number of actual route stops there are between the start and end of ptPath
	 */
	private int getNumberOfStopsInBetween(PTPath ptPath) {

		int n = 100;

		for(int i = 0; i < routeStops.size(); i++) {
			if(routeStops.get(i).getStopFacility().equals(ptPath.getFromStopFacility())) {
				for(int j = i+1; j < routeStops.size(); j++) {
					if(routeStops.get(j).getStopFacility().equals(ptPath.getToStopFacility())) {
						n = j-i-1;
					}
				}
			}
		}

		return n;
	}

	/**
	 * Link weight is calculated for every link in every ptPath
	 * @return the link weights for the current transit route
	 */
	public Map<Id<Link>, Double> getSimpleWeights() {
		for(PTPath ptPath : ptPaths.values()) {
			double pathWeight = getLinkWeight(ptPath);
			for (Id<Link> linkId : ptPath.getLinkIds()) {
				MapUtils.addToDouble(linkId, weights, pathWeight, pathWeight);
			}
		}

		return weights;
	}

	/**
	 * Calculates the link weight based on the travel time of the whole PTpath
	 *
	 * weight = 3600 - travelTime
	 *
	 * @param ptPath the path from one stopfacility to another
	 * @return the link weight
	 */
	public static double getLinkWeight(PTPath ptPath) {
		return getLinkWeight(ptPath, 3600.0);
	}

	/**
	 * Calculates the link weight based on the travel time of the whole PTpath.
	 * uses a reference time
	 *
	 * weight = reference - travelTime
	 *
	 * @param ptPath
	 * @return
	 */
	public static double getLinkWeight(PTPath ptPath, double reference) {
		double weight = reference - ptPath.getInterTravelTime();
		return (weight > 0 ? weight : 0);
	}
}
