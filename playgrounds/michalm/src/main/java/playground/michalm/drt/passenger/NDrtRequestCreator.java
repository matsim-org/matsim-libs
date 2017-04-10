/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.michalm.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.utils.geometry.CoordUtils;

import com.google.inject.Inject;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.run.DrtConfigGroup;

/**
 * @author michalm
 */
public class NDrtRequestCreator implements PassengerRequestCreator {
	private final DrtConfigGroup drtCfg;

	@Inject
	public NDrtRequestCreator(DrtConfigGroup drtCfg) {
		this.drtCfg = drtCfg;
	}

	@Override
	public NDrtRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {
		double latestDepartureTime = departureTime + drtCfg.getMaxWaitTime();

		double estimatedDistance = drtCfg.getEstimatedBeelineDistanceFactor()
				* CoordUtils.calcEuclideanDistance(fromLink.getCoord(), toLink.getCoord());
		double estimatedTravelTime = estimatedDistance / drtCfg.getEstimatedDrtSpeed();
		double maxTravelTime = drtCfg.getMaxTravelTimeAlpha() * estimatedTravelTime + drtCfg.getMaxTravelTimeBeta();
		double latestArrivalTime = departureTime + maxTravelTime;

		return new NDrtRequest(id, passenger, fromLink, toLink, departureTime, latestDepartureTime, latestArrivalTime,
				submissionTime);
	}
}
