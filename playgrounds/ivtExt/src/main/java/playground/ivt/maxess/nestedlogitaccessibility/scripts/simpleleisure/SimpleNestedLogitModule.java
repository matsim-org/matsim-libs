/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ivt.maxess.nestedlogitaccessibility.scripts.simpleleisure;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.Scenario;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;
import playground.ivt.maxess.nestedlogitaccessibility.scripts.ModeNests;
import playground.ivt.maxess.nestedlogitaccessibility.scripts.NestedAccessibilityConfigGroup;
import playground.ivt.maxess.prepareforbiogeme.tripbased.mikrozensus.RunMzTripChoiceSetConversion;
import playground.ivt.router.TripSoftCache;
import playground.ivt.utils.ConcurrentStopWatch;

/**
 * @author thibautd
 */
public class SimpleNestedLogitModule extends AbstractModule {
	public final ConcurrentStopWatch<SimpleNestedLogitModelChoiceSetIdentifier.Measurement> stopWatch =
			new ConcurrentStopWatch<>( SimpleNestedLogitModelChoiceSetIdentifier.Measurement.class );

	// only one cache across threads
	private final TripSoftCache cache =
			new TripSoftCache(
					false,
					TripSoftCache.LocationType.link );

	@Override
	protected void configure() {
		bind( new TypeLiteral<Utility<ModeNests>>() {} )
				.to( SimpleNestedLogitModelUtility.class );
		bind( new TypeLiteral<ChoiceSetIdentifier<ModeNests>>() {} )
				.to( SimpleNestedLogitModelChoiceSetIdentifier.class );
	}

	@Provides
	public SimpleNestedLogitModelChoiceSetIdentifier createChoiceSetIdentifier( final Scenario scenario ) {
		final NestedAccessibilityConfigGroup group = (NestedAccessibilityConfigGroup)
				scenario.getConfig().getModule( NestedAccessibilityConfigGroup.GROUP_NAME );
		return new SimpleNestedLogitModelChoiceSetIdentifier(
				(SimpleNestedLogitUtilityConfigGroup)  scenario.getConfig().getModule( SimpleNestedLogitUtilityConfigGroup.GROUP_NAME ),
				stopWatch,
				group.getActivityType(),
				group.getChoiceSetSize(),
				RunMzTripChoiceSetConversion.createTripRouter(
						scenario,
						cache ),
				scenario.getActivityFacilities(),
				scenario.getPopulation().getPersonAttributes(),
				group.getDistanceBudget() );
	}
}
