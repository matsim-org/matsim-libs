/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeEstimatorWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.costestimators;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeData;
import org.matsim.core.trafficmonitoring.TravelTimeDataArrayFactory;
import org.matsim.core.trafficmonitoring.TravelTimeDataFactory;

/**
 * Class acting as an intermediate between a client (a leg travel time estimator)
 * and the "global" TravelTimeCalculator.
 *
 * <br>
 * The idea behind this class is to reduce the incredibly expensive calls to the
 * TravelTimeCalculator getters, which result in map lookups over the whole network,
 * by storing already examined results.
 *
 * <br>
 * This class is meant to be instanciated for each plan, and is not
 * thread safe (contrary to the global TravelTimeCalculator)
 *
 * @author thibautd
 */
public class TravelTimeEstimatorWrapper implements TravelTime {
	private final TravelTime travelTime;
	private final double binSize;
	private final int numSlots;

	private static final double LATE_TT = Double.POSITIVE_INFINITY;

	// TODO: change array for an ad-hoc structure
	private final Map<Id, double[]> dataPerLink = new HashMap<Id, double[]>();

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public TravelTimeEstimatorWrapper(
			final TravelTimeCalculator travelTime) {
		this.travelTime = travelTime;
		this.binSize = travelTime.getTimeSlice();
		this.numSlots = travelTime.getNumSlots();
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public double getLinkTravelTime(final Link link, final double time) {
		Id linkId = link.getId();
		int index = (int) (time / binSize); 

		if (index >= numSlots) {
			return travelTime.getLinkTravelTime(link, time);
		}

		double[] travelTimes = dataPerLink.get(linkId);

		// create
		if (travelTimes == null) {
			travelTimes = new double[numSlots];

			for (int i=0; i < numSlots; i++) {
				travelTimes[i] = -1;
			}

			dataPerLink.put(linkId, travelTimes);
		}

		if (travelTimes[index] < 0) {
			travelTimes[index] = travelTime.getLinkTravelTime(link, time);
		}

		return travelTimes[index];
	}
}

