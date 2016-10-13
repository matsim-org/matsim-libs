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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.controler.Injector;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSampler;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SimpleSnowballModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballSamplingConfigGroup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * @author thibautd
 */
public class RunScalabilityAnalysis {

	public static void main( String[] args ) {
		final ScalabilityConfigGroup scalabilityConfigGroup = new ScalabilityConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , scalabilityConfigGroup );
		MoreIOUtils.initOut( config.controler().getOutputDirectory() );

		try ( final ScalabilityStatisticsListener statsListenner = new ScalabilityStatisticsListener( config.controler().getOutputDirectory()+"/stats.dat"  )) {

			for ( double sample : scalabilityConfigGroup.getSamples() ) {
				for ( int tryNr = 0; tryNr < scalabilityConfigGroup.getnTries(); tryNr++ ) {
					final SnowballSamplingConfigGroup configGroup = new SnowballSamplingConfigGroup();
					final Config tryConfig = ConfigUtils.loadConfig( args[ 0 ], configGroup );

					final String outputDir = config.controler().getOutputDirectory() + "/tmp/";
					config.controler().setOutputDirectory( outputDir );
					MoreIOUtils.deleteDirectoryIfExists( outputDir );

					statsListenner.startTry( sample , tryNr );

					final SocialNetwork network = runTry( tryConfig , sample , statsListenner );

					statsListenner.endTry( network );
				}
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}

	private static SocialNetwork runTry( final Config config,
			final double sample,
			final ScalabilityStatisticsListener statsListenner ) {
		try ( final AutocloserModule closer = new AutocloserModule() ) {
			final Scenario scenario = ScenarioUtils.loadScenario( config );
			samplePopulation( scenario , sample );
			final com.google.inject.Injector injector =
					Injector.createInjector( config,
							closer,
							new SimpleSnowballModule( config ),
							new ScenarioByInstanceModule( scenario ),
							b -> b.bind( SocialNetworkSampler.class ),
							b -> b.bind( ScalabilityStatisticsListener.class ).toInstance( statsListenner ) );

			return injector.getInstance( SocialNetworkSampler.class ).sampleSocialNetwork();
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	private static final Random random = new Random( 123 );
	private static void samplePopulation(
			final Scenario scenario,
			final double sample ) {
		final Set<Id<Person>> toRemove = new HashSet<>();

		for ( Id<Person> p : scenario.getPopulation().getPersons().keySet() ) {
			if ( random.nextDouble() > sample ) toRemove.add( p );
		}

		for ( Id<Person> p : toRemove ) scenario.getPopulation().removePerson( p );
	}

	private static class ScalabilityConfigGroup extends ReflectiveConfigGroup {
		private double basisSample = 0.1;
		private double[] samples = {0.125,0.25,0.5,1};
		private int nTries = 1;

		public ScalabilityConfigGroup() {
			super( "scalability" );
		}

		@StringGetter("samples")
		private String getSamplesString() {
			return Arrays.toString( getSamples() );
		}

		public double[] getSamples() {
			return samples;
		}

		@StringSetter("samples")
		public void setSamples( final String samples ) {
			setSamples(
					Arrays.stream( CollectionUtils.stringToArray( samples ) )
							.mapToDouble( Double::parseDouble )
							.toArray() );
		}

		public void setSamples( final double[] samples ) {
			this.samples = samples;
		}

		@StringGetter("nTries")
		public int getnTries() {
			return nTries;
		}

		@StringSetter("nTries")
		public void setnTries( final int nTries ) {
			this.nTries = nTries;
		}

		@StringGetter("basisSample")
		public double getBasisSample() {
			return basisSample;
		}

		@StringSetter("basisSample")
		public void setBasisSample( final double basisSample ) {
			this.basisSample = basisSample;
		}
	}
}
