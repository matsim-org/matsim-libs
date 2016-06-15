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
package playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.scripts.simplemikrozansusconstrainedaccessibility;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.MainModeIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.ConstrainedAccessibilityConfigGroup;
import playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.CorrectedUtilityCreator;
import playground.ivt.maxess.nestedlogitaccessibility.depalmaconstrained.SingleNest;
import playground.ivt.maxess.nestedlogitaccessibility.framework.ChoiceSetIdentifier;
import playground.ivt.maxess.nestedlogitaccessibility.framework.NestedLogitModel;
import playground.ivt.maxess.nestedlogitaccessibility.framework.Utility;
import playground.ivt.maxess.nestedlogitaccessibility.scripts.NestedAccessibilityConfigGroup;

/**
 * @author thibautd
 */
public class SimpleConstrainedLogitModule extends AbstractModule {
	private static final Logger log = Logger.getLogger( SimpleConstrainedLogitModule.class );

	@Override
	protected void configure() {
		//bind( new TypeLiteral<Utility<SingleNest>>() {} );
		bind( new TypeLiteral<ChoiceSetIdentifier<SingleNest>>() {} )
				.to( SimpleChoiceSetIdentifier.class );
	}

	@Provides @Singleton
	private Utility<SingleNest> createUtility(
			final Scenario scenario,
			final ConstrainedAccessibilityConfigGroup configGroup,
			final NestedAccessibilityConfigGroup accessibilityConfigGroup,
			final UtilityConfigGroup utilityConfigGroup,
			final MainModeIdentifier modeIdentifier,
			final ChoiceSetIdentifier<SingleNest> choiceSetIdentifier ) {
		log.info( "start creating corrected utility");

		log.info( "initialize base utility");
		final Utility<SingleNest> baseUtility =
				new SimpleUtility(
						utilityConfigGroup,
						scenario.getPopulation().getPersonAttributes(),
						modeIdentifier );

		if ( !configGroup.isUseCapacityConstraints() ) {
			log.info( "compute accessibility WITHOUT capcity correction" );
			return baseUtility;
		}

		log.info( "initialize corrector");
		final CorrectedUtilityCreator<SingleNest> creator =
				new CorrectedUtilityCreator<>(
						configGroup,
						scenario,
						accessibilityConfigGroup.getActivityType() );

		log.info( "start correcting");
		return creator.createCorrectedUtility(
				new NestedLogitModel<>(
						baseUtility,
						choiceSetIdentifier ) );
	}
}
