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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
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
public class DvrpOfflineTravelTimeEstimator
		implements DvrpTravelTimeEstimator, MobsimBeforeCleanupListener, AfterMobsimListener {
	private final TravelTime observedTT;
	private final Network network;
	private final OutputDirectoryHierarchy outputDirectoryHierarchy;

	private final TimeDiscretizer timeDiscretizer;
	private final int intervalCount;
	private final double timeInterval;

	private final double[][] linkTravelTimes;
	private final double alpha;

	private final String delimiter;

	@Inject
	public DvrpOfflineTravelTimeEstimator(@Named(DvrpTravelTimeModule.DVRP_INITIAL) TravelTime initialTT,
										  @Named(DvrpTravelTimeModule.DVRP_OBSERVED) TravelTime observedTT,
										  @Named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING) Network network,
										  TravelTimeCalculatorConfigGroup ttCalcConfig, DvrpConfigGroup dvrpConfig,
										  OutputDirectoryHierarchy outputDirectoryHierarchy, GlobalConfigGroup globalConfig) {
		this(initialTT, observedTT, network, new TimeDiscretizer(ttCalcConfig), dvrpConfig.travelTimeEstimationAlpha,
				outputDirectoryHierarchy, globalConfig.getDefaultDelimiter());
	}

	public DvrpOfflineTravelTimeEstimator(TravelTime initialTT, TravelTime observedTT, Network network,
			TimeDiscretizer timeDiscretizer, double travelTimeEstimationAlpha,
			OutputDirectoryHierarchy outputDirectoryHierarchy, String delimiter) {
		this.observedTT = observedTT;
		this.network = network;

		this.timeDiscretizer = timeDiscretizer;
		this.intervalCount = timeDiscretizer.getIntervalCount();
		this.timeInterval = timeDiscretizer.getTimeInterval();

		this.outputDirectoryHierarchy = outputDirectoryHierarchy;
		this.delimiter = delimiter;

		alpha = travelTimeEstimationAlpha;
		checkArgument(alpha >= 0 && alpha <= 1, "travelTimeEstimationAlpha must be in [0,1]");

		linkTravelTimes = DvrpOfflineTravelTimes.convertToLinkTravelTimeMatrix(initialTT, network.getLinks().values(),
				timeDiscretizer);
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		// TODO TTC is more flexible (simple averaging vs linear interpolation, etc.)

		// check if the link belongs to the network
		var linkTT = checkNotNull(linkTravelTimes[link.getId().index()],
				"Link (%s) does not belong to network. No travel time data.", link.getId());

		int timeBin = getIdx(time);
		return linkTT[timeBin];
	}

	private int getIdx(double time) {
		return TimeBinUtils.getTimeBinIndex(time, timeInterval, intervalCount);
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		if(alpha > 0) {
			updateTTs(observedTT, alpha);
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		DvrpOfflineTravelTimes.saveLinkTravelTimes(timeDiscretizer, linkTravelTimes,
				outputDirectoryHierarchy.getIterationFilename(event.getIteration(),
						"dvrp_travel_times.csv.gz"), delimiter);
	}

	private void updateTTs(TravelTime travelTime, double alpha) {
		for (Link link : network.getLinks().values()) {
			double[] tt = linkTravelTimes[link.getId().index()];
			timeDiscretizer.forEach((bin, time) -> {
				double oldEstimatedTT = tt[bin];
				double experiencedTT = travelTime.getLinkTravelTime(link, time, null, null);
				tt[bin] = alpha * experiencedTT + (1 - alpha) * oldEstimatedTT;
			});
		}
	}
}
