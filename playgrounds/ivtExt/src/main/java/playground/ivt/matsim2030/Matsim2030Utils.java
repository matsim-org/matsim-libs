/* *********************************************************************** *
 * project: org.matsim.*
 * Matsim2030Utils.java
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
package playground.ivt.matsim2030;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.XY2Links;

import herbie.running.controler.listeners.CalcLegTimesHerbieListener;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.matsim2030.generation.ScenarioMergingConfigGroup;
import playground.ivt.matsim2030.router.TransitRouterNetworkReader;
import playground.ivt.matsim2030.router.TransitRouterWithThinnedNetworkFactory;
import playground.ivt.matsim2030.scoring.MATSim2010ScoringFunctionFactory;
import playground.ivt.utils.TripModeShares;

/**
 * @author thibautd
 */
public class Matsim2030Utils {
	private static final Logger log =
		Logger.getLogger(Matsim2030Utils.class);

	
	private static final String CALC_LEG_TIMES_FILE_NAME = "calcLegTimes.txt";
	private static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "multimodalLegDistanceDistribution.txt";
	private static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";


	public static Config loadConfig( final String file ) {
		final Config config = ConfigUtils.createConfig();
		addDefaultGroups( config );
		ConfigUtils.loadConfig( config , file );
		return config;
	}

	public static void addDefaultGroups( final Config config ) {
		config.addModule( new ScenarioMergingConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
	}

	public static Scenario loadScenario( final Config config ) {
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final ScenarioMergingConfigGroup mergingGroup = (ScenarioMergingConfigGroup)
			config.getModule( ScenarioMergingConfigGroup.GROUP_NAME );

		if ( mergingGroup.getCrossBorderPlansFile() != null ) {
			log.info( "reading cross border plans from "+mergingGroup.getCrossBorderPlansFile() );
			addSubpopulation(
					mergingGroup.getCrossBorderPopulationId(),
					mergingGroup.getCrossBorderPlansFile(),
					scenario );
		}

		if ( mergingGroup.getCrossBorderFacilitiesFile() != null ) {
			log.info( "reading facilities for cross-border population from "+mergingGroup.getCrossBorderFacilitiesFile() );
			new MatsimFacilitiesReader( scenario ).readFile( mergingGroup.getCrossBorderFacilitiesFile() );
		}

		if ( mergingGroup.getFreightPlansFile() != null ) {
			log.info( "reading freight plans from "+mergingGroup.getFreightPlansFile() );
			addSubpopulation(
					mergingGroup.getFreightPopulationId(),
					mergingGroup.getFreightPlansFile(),
					scenario );
		}

		if ( mergingGroup.getFreightFacilitiesFile() != null ) {
			log.info( "reading facilities for freight population from "+mergingGroup.getFreightFacilitiesFile() );
			new MatsimFacilitiesReader( scenario ).readFile( mergingGroup.getFreightFacilitiesFile() );
		}

		// do it BEFORE importing the PT part of the network.
		connectFacilitiesWithLinks( scenario );

		if ( mergingGroup.getPtSubnetworkFile() != null ) {
			log.info( "reading pt network from "+mergingGroup.getPtSubnetworkFile() );
			new MatsimNetworkReader( scenario ).readFile( mergingGroup.getPtSubnetworkFile() );
		}

		return scenario;
	}

	private static void addSubpopulation(
			final String subpopulationName,
			final String subpopulationFile,
			final Scenario scenario ) {
		// we could read directly the population in the global scenario,
		// but this would make creation of object attributes tricky.
		final Scenario tempSc = ScenarioUtils.createScenario( scenario.getConfig() );
		new MatsimPopulationReader( tempSc ).readFile( subpopulationFile );

		final String attribute = scenario.getConfig().plans().getSubpopulationAttributeName();
		for ( Person p : tempSc.getPopulation().getPersons().values() ) {
			scenario.getPopulation().addPerson( p );
			scenario.getPopulation().getPersonAttributes().putAttribute(
					p.getId().toString(),
					attribute,
					subpopulationName );
		}
	}

	private static void connectFacilitiesWithLinks( final Scenario sc ) {
		final StageActivityTypes stages = new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE );
		final PersonAlgorithm xy2Links = new XY2Links( sc );

		// first: if there are links indicated in the activities, use them
		for ( Person person : sc.getPopulation().getPersons().values() ) {
			// allocate links to activities which have none
			xy2Links.run( person );

			// use links of activities to locate facilities
			for ( Activity act : TripStructureUtils.getActivities( person.getSelectedPlan(), stages ) ) {
				final Id linkId = act.getLinkId();
				final Id facilityId = act.getFacilityId();

				final ActivityFacility fac = sc.getActivityFacilities().getFacilities().get( facilityId );
				if ( fac.getLinkId() == null ) ((ActivityFacilityImpl) fac).setLinkId( linkId );
				else if ( !fac.getLinkId().equals( linkId ) ) throw new RuntimeException( "inconsistent links for facility "+facilityId );
			}
		}

		// now hopefully everything is nice and consistent...
	}

	public static void initializeLocationChoice( final Controler controler ) {
		final Scenario scenario = controler.getScenario();
		final DestinationChoiceBestResponseContext lcContext =
			new DestinationChoiceBestResponseContext( scenario );
		lcContext.init();

		// XXX this thing is awful. I think one can (and should) avoid using it...
		// There does not seem to be a reason not to do all the notify startup
		// method does before calling run().
		controler.addControlerListener(
				new DestinationChoiceInitializer(
					lcContext));
	}

	public static void initializeScoring( final Controler controler ) {
 		final MATSim2010ScoringFunctionFactory scoringFunctionFactory =
			new MATSim2010ScoringFunctionFactory(
					controler.getScenario(),
					new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ) ); 	
		controler.setScoringFunctionFactory( scoringFunctionFactory );
	}

	public static TripRouterFactory createTripRouterFactory( final Scenario scenario ) {
		final TransitRouterConfig conf = new TransitRouterConfig( scenario.getConfig() );

		final ScenarioMergingConfigGroup matsim2030conf =
			(ScenarioMergingConfigGroup)
			scenario.getConfig().getModule(
					ScenarioMergingConfigGroup.GROUP_NAME );
		
		final TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		
		if ( matsim2030conf.getThinnedTransitRouterNetworkFile() != null ) {
			log.info( "using thinned transit router network from "+matsim2030conf.getThinnedTransitRouterNetworkFile() );
			final TransitRouterNetwork transitRouterNetwork = new TransitRouterNetwork();
			new TransitRouterNetworkReader(
					scenario,
					scenario.getTransitSchedule(),
					transitRouterNetwork ).parse(
						matsim2030conf.getThinnedTransitRouterNetworkFile() );
	
			final TransitRouterWithThinnedNetworkFactory transitRouterFactory =
				new TransitRouterWithThinnedNetworkFactory(
						scenario.getTransitSchedule(),
						conf,
						transitRouterNetwork );
	
			builder.setTransitRouterFactory( transitRouterFactory );
		}
		else {
			log.warn( "using no pre-processed transit router network --- This would be more efficient!" );
		}

		return builder.build( scenario );
	}

	public static void loadControlerListeners( final Controler controler ) {
		controler.addControlerListener(
				new CalcLegTimesHerbieListener(
					CALC_LEG_TIMES_FILE_NAME,
					LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		controler.addControlerListener(
				new LegDistanceDistributionWriter(
					LEG_DISTANCE_DISTRIBUTION_FILE_NAME,
					controler.getScenario().getNetwork()));
		final TripRouter router = controler.getTripRouterFactory().instantiateAndConfigureTripRouter();
		controler.addControlerListener(
				new TripModeShares(
					25, // write interval. TODO: pass by config
					controler.getControlerIO(),
					controler.getScenario(),
					router.getMainModeIdentifier(),
					router.getStageActivityTypes() ) );
	}

	public static void createEmptyDirectoryOrFailIfExists(final String directory) {
		final File file = new File( directory +"/" );
		if ( file.exists() && file.list().length > 0 ) {
			throw new UncheckedIOException( "Directory "+directory+" exists and is not empty!" );
		}
		file.mkdirs();
	}
}

