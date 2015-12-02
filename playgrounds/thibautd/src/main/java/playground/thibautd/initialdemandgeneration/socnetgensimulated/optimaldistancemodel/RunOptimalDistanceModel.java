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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.optimaldistancemodel;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.arentzemodel.ArentzePopulationModule;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.ConfigModule;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.FileWriterEvolutionListener;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.ModelIterator;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.PreprocessedModelRunnerConfigGroup;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.SocNetGenFrameworkModule;
import playground.thibautd.initialdemandgeneration.socnetgensimulated.framework.SocialNetworkGenerationConfigGroup;
import playground.thibautd.utils.MoreIOUtils;

/**
 * @author thibautd
 */
public class RunOptimalDistanceModel {
	private static final Logger log = Logger.getLogger(RunOptimalDistanceModel.class);
	
	public static void main(final String[] args) {
		final String configFile = args[ 0 ];

		final Injector injector = Guice.createInjector(
				new SocNetGenFrameworkModule(),
				new ConfigModule(
						configFile,
						OptimalDistanceConfigGroup.class),
				new OptimalDistanceTieUtilityModule(),
				new ArentzePopulationModule());

		final SocialNetworkGenerationConfigGroup config = injector.getInstance( SocialNetworkGenerationConfigGroup.class );
		final PreprocessedModelRunnerConfigGroup runnerConfig = injector.getInstance( PreprocessedModelRunnerConfigGroup.class );

		MoreIOUtils.initOut(config.getOutputDirectory());

		log.info( "################################################################################" );
		log.info( "###### start socnet gen" );
		log.info( "###### popfile: " + config.getInputPopulationFile() );
		log.info( "###### outputdir: " + config.getOutputDirectory() );
		log.info( "###### primary sampling rate: " + runnerConfig.getPrimarySampleRate() );
		log.info( "###### secondary sampling rate: " + runnerConfig.getSecondarySampleRate());
		log.info( "################################################################################" );

		final ModelIterator modelIterator = injector.getInstance( ModelIterator.class );

		final FileWriterEvolutionListener fileListener = new FileWriterEvolutionListener( config.getOutputDirectory() + "/threshold-evolution.dat" );
		modelIterator.addListener( fileListener );

		final SocialNetwork network = modelIterator.iterateModelToTarget();

		fileListener.close();
		new SocialNetworkWriter( network ).write( config.getOutputDirectory() + "/social-network.xml.gz" );

		MoreIOUtils.closeOutputDirLogging();
	}
}
