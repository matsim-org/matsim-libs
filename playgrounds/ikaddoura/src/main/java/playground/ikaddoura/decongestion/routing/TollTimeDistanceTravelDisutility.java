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

package playground.ikaddoura.decongestion.routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.decongestion.data.DecongestionInfo;

/**
 * A cost calculator which respects time, distance and decongestion tolls. 
 *
 * @author ikaddoura
 */
public final class TollTimeDistanceTravelDisutility implements TravelDisutility {
	private static final Logger log = Logger.getLogger(TollTimeDistanceTravelDisutility.class);

	private final TravelDisutility delegate;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	private final DecongestionInfo info;
	private final double sigma;

	TollTimeDistanceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double sigma, DecongestionInfo info) {
		this.info = info;
		this.cnScoringGroup = cnScoringGroup;
		this.sigma = sigma;
		
		final RandomizingTimeDistanceTravelDisutilityFactory builder = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, cnScoringGroup );
		builder.setSigma(sigma);
		this.delegate = builder.createTravelDisutility(timeCalculator);

		log.info("Using the toll-adjusted travel disutility in the decongestion package.");
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		int timeBin = (int) (time / info.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());

		double timeDistanceTravelDisutilityFromDelegate = this.delegate.getLinkTravelDisutility(link, time, person, vehicle);
				
		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}

		// adjust the travel disutility for the toll
		double toll = 0.;
		if (info.getlinkInfos().containsKey(link.getId()) && info.getlinkInfos().get(link.getId()).getTime2toll().containsKey(timeBin)) {
			toll = info.getlinkInfos().get(link.getId()).getTime2toll().get(timeBin);
		}
		double tollAdjustedLinkTravelDisutility = Double.NEGATIVE_INFINITY;
		tollAdjustedLinkTravelDisutility = timeDistanceTravelDisutilityFromDelegate + logNormalRnd * cnScoringGroup.getMarginalUtilityOfMoney() * toll;
		
		return tollAdjustedLinkTravelDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return this.delegate.getLinkMinimumTravelDisutility(link);
	}

}
