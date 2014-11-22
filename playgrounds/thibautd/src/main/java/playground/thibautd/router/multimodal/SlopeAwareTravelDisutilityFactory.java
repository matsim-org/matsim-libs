/* *********************************************************************** *
 * project: org.matsim.*
 * SlopeAwareTravelDisutilityFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.router.multimodal;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author thibautd
 */
public class SlopeAwareTravelDisutilityFactory implements TravelDisutilityFactory {
	final TravelDisutilityFactory delegateFactory;
	final DenivelationAlongRouteScoring slopeScoring;

	public SlopeAwareTravelDisutilityFactory(
			final double betaGain,
			final Map<Id<Link>, Double> slopes,
			final TravelDisutilityFactory delegate) {
		this(
			new DenivelationAlongRouteScoring( null , slopes , null , null , betaGain ),
			delegate );
	}

	public SlopeAwareTravelDisutilityFactory(
			final DenivelationAlongRouteScoring slopeScoring,
			final TravelDisutilityFactory delegate) {
		this.delegateFactory = delegate;
		this.slopeScoring = slopeScoring;
	}

	@Override
	public TravelDisutility createTravelDisutility(
			final TravelTime timeCalculator,
			final PlanCalcScoreConfigGroup cnScoringGroup ) {
		final TravelDisutility delegate = delegateFactory.createTravelDisutility( timeCalculator , cnScoringGroup );

		return new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(
					final Link link,
					final double time,
					final Person person,
					final Vehicle vehicle ) {
				final double baseDisutility =
					delegate.getLinkTravelDisutility(
							link,
							time,
							person,
							vehicle );

				return baseDisutility - slopeScoring.calcGainUtil( link );
			}

			@Override
			public double getLinkMinimumTravelDisutility( final Link link ) {
				return delegate.getLinkMinimumTravelDisutility( link );
			}
		};
	}
}

