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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.estimatedsampling;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import playground.ivt.utils.MoreIOUtils;
import playground.ivt.utils.SoftCache;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel.ArentzePopulation;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.ConfigModule;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.ModelRunner;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.PreprocessedModelRunnerConfigGroup;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.SocNetGenFrameworkModule;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.SocialNetworkGenerationConfigGroup;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.Thresholds;

/**
 * @author thibautd
 */
public class RunModel {
	private static final Logger log =
		Logger.getLogger(RunModel.class);


	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Injector injector = Guice.createInjector(
				new SocNetGenFrameworkModule(),
				new ConfigModule(
						configFile,
						EstimatedSamplingModelConfigGroup.class ),
				new ContinuousAgeArentzeTieUtilityModule(),
				new ContinuousAgeArentzePopulationModule());

		Logger.getLogger( SoftCache.class ).setLevel(Level.TRACE);
		final SocialNetworkGenerationConfigGroup config = injector.getInstance( SocialNetworkGenerationConfigGroup.class );
		final PreprocessedModelRunnerConfigGroup runnerConfig = injector.getInstance( PreprocessedModelRunnerConfigGroup.class );
		final EstimatedSamplingModelConfigGroup modelConfig =  injector.getInstance( EstimatedSamplingModelConfigGroup.class );
		// convert primary group size in probability. Actual size of sample varies to take into account reflectivity
		runnerConfig.setPrimarySampleRate( modelConfig.getPrimarySample() / injector.getInstance( ArentzePopulation.class ).size() );

		MoreIOUtils.initOut(config.getOutputDirectory());

		log.info( "################################################################################" );
		log.info( "###### start socnet gen" );
		log.info( "###### popfile: " + config.getInputPopulationFile() );
		log.info( "###### outputdir: " + config.getOutputDirectory() );
		log.info( "###### primary sampling rate: " + runnerConfig.getPrimarySampleRate() );
		log.info( "###### secondary sampling rate: " + runnerConfig.getSecondarySampleRate());
		log.info( "################################################################################" );

		final ModelRunner modelRunner = injector.getInstance( ModelRunner.class );

		final SocialNetwork network = modelRunner.runModel(
				new Thresholds(
						config.getInitialPrimaryThreshold(),
						config.getInitialSecondaryReduction() ) );

		new SocialNetworkWriter( network ).write( config.getOutputDirectory() + "/social-network.xml.gz" );

		MoreIOUtils.closeOutputDirLogging();
	}
}

