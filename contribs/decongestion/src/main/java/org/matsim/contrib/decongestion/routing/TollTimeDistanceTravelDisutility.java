/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.decongestion.routing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import org.matsim.contrib.decongestion.data.DecongestionInfo;
import org.matsim.contrib.decongestion.data.LinkInfo;

/**
 * A cost calculator which respects time, distance and decongestion tolls.
 *
 * @author ikaddoura
 */
final class TollTimeDistanceTravelDisutility implements TravelDisutility {
	private static final Logger log = LogManager.getLogger(TollTimeDistanceTravelDisutility.class);

	private final TravelDisutility delegate;
	private final DecongestionInfo info;
	private final double timeBinSize;
	private final double marginalUtilityOfMoney;
	private final double sigma;

	TollTimeDistanceTravelDisutility( final TravelTime timeCalculator, Config config, DecongestionInfo info ) {
		this.info = info;
		this.marginalUtilityOfMoney = config.scoring().getMarginalUtilityOfMoney();

		final RandomizingTimeDistanceTravelDisutilityFactory builder = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config );
                this.delegate = builder.createTravelDisutility(timeCalculator);

		this.timeBinSize = info.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize();

		this.sigma = config.routing().getRoutingRandomness();

		log.info("Using the toll-adjusted travel disutility (improved version) in the decongestion package.");
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		int timeBin = (int) (time / timeBinSize);

		double timeDistanceTravelDisutilityFromDelegate = this.delegate.getLinkTravelDisutility(link, time, person, vehicle);

		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}

		// adjust the travel disutility for the toll
		double toll = 0.;

		LinkInfo linkInfo = info.getlinkInfos().get(link.getId());
		if (linkInfo != null) {

			Double linkInfoTimeBinToll = linkInfo.getTime2toll().get(timeBin);
			if (linkInfoTimeBinToll != null) {
				toll = linkInfoTimeBinToll;
			}
		}

		double tollAdjustedLinkTravelDisutility = timeDistanceTravelDisutilityFromDelegate + logNormalRnd * marginalUtilityOfMoney * toll;
		return tollAdjustedLinkTravelDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return this.delegate.getLinkMinimumTravelDisutility(link);
	}

}
