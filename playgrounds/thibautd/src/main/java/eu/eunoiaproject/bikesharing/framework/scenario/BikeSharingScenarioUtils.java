/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingScenarioUtils.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import java.util.Arrays;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.util.LinkSlopesReader;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.thibautd.router.multimodal.AccessEgressMultimodalTripRouterFactory;
import playground.thibautd.router.multimodal.AccessEgressNetworkBasedTeleportationRouteFactory;
import playground.thibautd.utils.CollectionUtils;

import eu.eunoiaproject.bikesharing.framework.BikeSharingConstants;
import eu.eunoiaproject.bikesharing.framework.router.BikeSharingTripRouterFactory;

/**
 * Provides helper methods to load a bike sharing scenario.
 * Using this class is by no means necessary, but simplifies
 * the writing of scripts.
 *
 * @author thibautd
 */
public class BikeSharingScenarioUtils {
	public static final String LINK_SLOPES_ELEMENT_NAME = "linkSlopes";

	private  BikeSharingScenarioUtils() {}

	public static Config loadConfig( final String fileName , final Module... additionalModules ) {
		final Module[] modules = Arrays.copyOf( additionalModules , additionalModules.length + 1 );
		modules[ modules.length - 1 ] = new BikeSharingConfigGroup();
		final Config config = ConfigUtils.loadConfig(
				fileName,
				modules );

		if ( config.planCalcScore().getActivityParams( BikeSharingConstants.INTERACTION_TYPE ) == null ) {
			// not so nice...
			final ActivityParams params = new ActivityParams( BikeSharingConstants.INTERACTION_TYPE );
			params.setTypicalDuration( 120 );
			params.setOpeningTime( 0 );
			params.setClosingTime( 0 );
			config.planCalcScore().addActivityParams( params );
		}

		return config;
	}

	public static Scenario loadScenario( final Config config ) {
		// to make sure log entries are writen in log file
		OutputDirectoryLogging.catchLogEntries();
		final Scenario sc = ScenarioUtils.createScenario( config );
		configurePopulationFactory( sc );
		ScenarioUtils.loadScenario( sc );
		loadBikeSharingPart( sc );
		return sc;
	}

	public static void loadBikeSharingPart( final Scenario sc ) {
		final Config config = sc.getConfig();
		final BikeSharingConfigGroup confGroup = (BikeSharingConfigGroup)
			config.getModule( BikeSharingConfigGroup.GROUP_NAME );
		new BikeSharingFacilitiesReader( sc ).parse( confGroup.getFacilitiesFile() );

		final BikeSharingFacilities bsFacilities = (BikeSharingFacilities)
			sc.getScenarioElement( BikeSharingFacilities.ELEMENT_NAME );
		if ( confGroup.getFacilitiesAttributesFile() != null ) {
			new ObjectAttributesXmlReader( bsFacilities.getFacilitiesAttributes() ).parse(
					confGroup.getFacilitiesAttributesFile() );
		}

		final ActivityFacilities actFacilities = sc.getActivityFacilities();
		if ( CollectionUtils.intersects(
					actFacilities.getFacilities().keySet(),
					bsFacilities.getFacilities().keySet() ) ) {
			throw new RuntimeException( "ids of bike sharing stations and activity facilities overlap. This will cause problems!"+
					" Make sure Ids do not overlap, for instance by appending \"bs-\" at the start of all bike sharing facilities." );
		}

		final MultiModalConfigGroup multimodalConfigGroup = (MultiModalConfigGroup)
			sc.getConfig().getModule(
					MultiModalConfigGroup.GROUP_NAME );

		if ( multimodalConfigGroup != null ) {
			final Map<Id<Link>, Double> linkSlopes =
				new LinkSlopesReader().getLinkSlopes(
						multimodalConfigGroup,
						sc.getNetwork());
			sc.addScenarioElement( LINK_SLOPES_ELEMENT_NAME , linkSlopes );
		}

	}

	public static Scenario loadScenario( final String configFile , final Module... modules ) {
		return loadScenario( loadConfig( configFile , modules) );
	}

	public static void configurePopulationFactory( final Scenario scenario ) {
		final MultiModalConfigGroup multimodalConfigGroup = (MultiModalConfigGroup)
			scenario.getConfig().getModule(
					MultiModalConfigGroup.GROUP_NAME );

		final RouteFactory factory = new AccessEgressNetworkBasedTeleportationRouteFactory( );
		for (String mode : org.matsim.core.utils.collections.CollectionUtils.stringToArray(multimodalConfigGroup.getSimulatedModes())) {
			((PopulationFactoryImpl) scenario.getPopulation().getFactory()).setRouteFactory(mode, factory);
		}
	}

	public static TripRouterFactory createTripRouterFactoryAndConfigureRouteFactories(
			final TravelDisutilityFactory disutilityFactory,
			final Scenario scenario) {
		final MultiModalConfigGroup multimodalConfigGroup = (MultiModalConfigGroup)
			scenario.getConfig().getModule(
					MultiModalConfigGroup.GROUP_NAME );

		if ( !multimodalConfigGroup.isMultiModalSimulationEnabled() ) {
			return new BikeSharingTripRouterFactory( scenario );
		}

		// PrepareMultiModalScenario.run( scenario );

		final RouteFactory factory = new AccessEgressNetworkBasedTeleportationRouteFactory( );
        for (String mode : org.matsim.core.utils.collections.CollectionUtils.stringToArray(multimodalConfigGroup.getSimulatedModes())) {
			((PopulationFactoryImpl) scenario.getPopulation().getFactory()).setRouteFactory(mode, factory);
		}

		final Map<Id<Link>, Double> linkSlopes = (Map<Id<Link>, Double>)
			scenario.getScenarioElement( LINK_SLOPES_ELEMENT_NAME );
		final MultiModalTravelTimeFactory multiModalTravelTimeFactory =
			new MultiModalTravelTimeFactory(
					scenario.getConfig(),
					linkSlopes );
	
		return new AccessEgressMultimodalTripRouterFactory(
				scenario,
				multiModalTravelTimeFactory.createTravelTimes(),
				disutilityFactory,
				new BikeSharingTripRouterFactory( scenario ) );
	}
}

