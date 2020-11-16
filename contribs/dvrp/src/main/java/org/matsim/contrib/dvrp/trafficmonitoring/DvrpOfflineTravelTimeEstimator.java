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

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Used for offline estimation of travel times for VrpOptimizer by means of the exponential moving average. The
 * weighting decrease, alpha, must be in (0,1]. We suggest small values of alpha, e.g. 0.05.
 * <p>
 * The averaging starts from the initial travel time estimates. If not provided, the free-speed TTs is used as the
 * initial estimates
 *
 * @author michalm
 */
public class DvrpOfflineTravelTimeEstimator implements DvrpTravelTimeEstimator, MobsimBeforeCleanupListener {
	private final TravelTime observedTT;
	private final Network network;

	private final int interval;
	private final int intervalCount;
	private final double[][] linkTTs;
	private final double alpha;

	@Inject
	public DvrpOfflineTravelTimeEstimator(@Named(DvrpTravelTimeModule.DVRP_INITIAL) TravelTime initialTT,
			@Named(DvrpTravelTimeModule.DVRP_OBSERVED) TravelTime observedTT,
			@Named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING) Network network,
			TravelTimeCalculatorConfigGroup ttCalcConfig, DvrpConfigGroup dvrpConfig) {
		this(initialTT, observedTT, network, ttCalcConfig, dvrpConfig.getTravelTimeEstimationAlpha());
	}

	public DvrpOfflineTravelTimeEstimator(TravelTime initialTT, TravelTime observedTT, Network network,
			TravelTimeCalculatorConfigGroup ttCalcConfig, double travelTimeEstimationAlpha) {
		this.observedTT = observedTT;
		this.network = network;

		alpha = travelTimeEstimationAlpha;
		if (alpha > 1 || alpha <= 0) {
			throw new IllegalArgumentException("travelTimeEstimationAlpha must be in (0,1]");
		}

		interval = ttCalcConfig.getTraveltimeBinSize();
		intervalCount = TimeBinUtils.getTimeBinCount(ttCalcConfig.getMaxTime(), interval);

		linkTTs = new double[Id.getNumberOfIds(Link.class)][intervalCount];
		updateTTs(initialTT, 1.);
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		// TODO TTC is more flexible (simple averaging vs linear interpolation, etc.)

		//handle negative times (e.g. in backward shortest path search)
		int idx = Math.max(0, TimeBinUtils.getTimeBinIndex(time, interval, intervalCount));
		return linkTTs[link.getId().index()][idx];
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		updateTTs(observedTT, alpha);
	}

	private void updateTTs(TravelTime travelTime, double alpha) {
		for (Link link : network.getLinks().values()) {
			double[] tt = linkTTs[link.getId().index()];
			for (int i = 0; i < intervalCount; i++) {
				double oldEstimatedTT = tt[i];
				double experiencedTT = travelTime.getLinkTravelTime(link, i * interval, null, null);
				tt[i] = alpha * experiencedTT + (1 - alpha) * oldEstimatedTT;
			}
		}
	}
}
