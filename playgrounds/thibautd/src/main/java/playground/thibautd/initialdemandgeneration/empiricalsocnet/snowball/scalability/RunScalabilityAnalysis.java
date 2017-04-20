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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.scalability;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Injector;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.MonitoringUtils;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSampler;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSamplerModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SimpleSnowballModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballSamplingConfigGroup;

import java.util.Random;

/**
 * @author thibautd
 */
public class RunScalabilityAnalysis {

	public static void main( String[] args ) throws Exception {
		//MonitoringUtils.setMemoryLoggingOnGC();

		try ( AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose() ) {
		//try {
			final SnowballSamplingConfigGroup configGroup = new SnowballSamplingConfigGroup();
			final ScalabilityConfigGroup scalabilityConfigGroup = new ScalabilityConfigGroup();
			final Config config =
					ConfigUtils.loadConfig(
							args[ 0 ],
							configGroup,
							scalabilityConfigGroup );
			MoreIOUtils.initOut( config.controler().getOutputDirectory() );

			new ConfigWriter( config ).write( config.controler().getOutputDirectory() + "/output_config.xml" );
			final Scenario scenario = ScenarioUtils.loadScenario( config );

			final String outputDir = config.controler().getOutputDirectory();
			final String tmpDir = outputDir + "/tmp/";
			config.controler().setOutputDirectory( tmpDir );

			try ( final ScalabilityStatisticsListener statsListenner = new ScalabilityStatisticsListener( outputDir + "/stats.dat" , false ) ) {
				for ( double sample : scalabilityConfigGroup.getSamples() ) {
					for ( int tryNr = 0; tryNr < scalabilityConfigGroup.getnTries(); tryNr++ ) {
						MoreIOUtils.deleteDirectoryIfExists( tmpDir );

						final Scenario sampledScenario = samplePopulation( scenario, sample );

						statsListenner.startTry( sample, tryNr );

						final SocialNetwork network = runTry( sampledScenario, statsListenner );

						statsListenner.endTry( network );
					}
				}
			}
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	private static SocialNetwork runTry(
			final Scenario scenario,
			final ScalabilityStatisticsListener statsListenner ) {
		try ( final AutocloserModule closer = new AutocloserModule() ) {
			final com.google.inject.Injector injector =
					Injector.createInjector(
							scenario.getConfig(),
							closer,
							new SimpleSnowballModule( scenario.getConfig() ),
							new SocialNetworkSamplerModule( scenario ),
							b -> b.bind( ScalabilityStatisticsListener.class ).toInstance( statsListenner ) );

			return injector.getInstance( SocialNetworkSampler.class ).sampleSocialNetwork();
		}
		catch ( RuntimeException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	private static final Random random = new Random( 123 );
	private static Scenario samplePopulation(
			final Scenario scenario,
			final double sample ) {
		final Scenario sampled = ScenarioUtils.createScenario( scenario.getConfig() );

		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			if ( random.nextDouble() <= sample ) sampled.getPopulation().addPerson( p );
		}

		return sampled;
	}

}
