/* *********************************************************************** *
 * project: org.matsim.*
 * RunArentzeModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.*;
import playground.thibautd.utils.MoreIOUtils;
import playground.thibautd.utils.SoftCache;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * @author thibautd
 */
public class RunArentzeModel {
	private static final Logger log =
		Logger.getLogger(RunArentzeModel.class);


	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Injector injector = Guice.createInjector(
				new SocNetGenFrameworkModule(),
				new ConfigModule(
						configFile,
						TRBModelConfigGroup.class ),
				new ArentzeTieUtilityModule(),
				new ArentzePopulationModule());

		Logger.getLogger( SoftCache.class ).setLevel(Level.TRACE);
		final SocialNetworkGenerationConfigGroup config = injector.getInstance( SocialNetworkGenerationConfigGroup.class );
		final PreprocessedModelRunnerConfigGroup runnerConfig = injector.getInstance( PreprocessedModelRunnerConfigGroup.class );

		MoreIOUtils.initOut(config.getOutputDirectory());

		log.info( "################################################################################" );
		log.info( "###### start socnet gen" );
		log.info( "###### popfile: " + config.getInputPopulationFile() );
		log.info( "###### outputdir: " + config.getOutputDirectory() );
		log.info( "###### primary sampling rate: " + runnerConfig.getPrimarySampleRate() );
		log.info("###### secondary sampling rate: " + runnerConfig.getSecondarySampleRate());
		log.info( "################################################################################" );

		final Thresholds initialPoint =
				generateInitialPoint(
						injector.getInstance(TiesWeightDistribution.class),
						injector.getInstance( IndexedPopulation.class ).size(),
						config );

		final ModelIterator modelIterator = injector.getInstance( ModelIterator.class );

		final FileWriterEvolutionListener fileListener = new FileWriterEvolutionListener( config.getOutputDirectory() + "/threshold-evolution.dat" );
		modelIterator.addListener( fileListener );

		final SocialNetwork network = modelIterator.iterateModelToTarget( initialPoint );

		fileListener.close();
		new SocialNetworkWriter( network ).write( config.getOutputDirectory() + "/social-network.xml.gz" );

		MoreIOUtils.closeOutputDirLogging();
	}

	private static Thresholds generateInitialPoint(
			final TiesWeightDistribution distr ,
			final int populationSize,
			final SocialNetworkGenerationConfigGroup config ) {
		final double primary = Double.isNaN( config.getInitialPrimaryThreshold() ) ?
			generateHeuristicPrimaryThreshold( distr , populationSize , config.getTargetDegree() ) :
			config.getInitialPrimaryThreshold();
		final double secondary = Double.isNaN( config.getInitialSecondaryReduction() ) ?
			0 : config.getInitialPrimaryThreshold();

		final Thresholds thresholds = new Thresholds( primary , secondary );
		log.info( "initial thresholds: "+thresholds );
		return thresholds;
	}


	private static double generateHeuristicPrimaryThreshold(
			final TiesWeightDistribution distr ,
			final int populationSize,
			final double targetDegree ) {
		log.info( "generating heuristic initial points" );

		// rationale: sqrt(n) alters * sqrt(n) alters_of_alter
		final double target = Math.sqrt( targetDegree );
		return distr.findLowerBound( (long) (populationSize * target) );
	}


}

