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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.MatsimRandom;
import playground.ivt.utils.MonitoringUtils;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesCsvWriter;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSamplerUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.StopwatchCsvWriter;

/**
 * @author thibautd
 */
public class RunSimpleCliquesSampling {
	public static void main( final String[] args ) {
		final SnowballSamplingConfigGroup configGroup = new SnowballSamplingConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , configGroup  );

		MoreIOUtils.initOut( config.controler().getOutputDirectory() , config );

		new ConfigWriter( config ).write( config.controler().getOutputDirectory()+"/output_config.xml" );

		MatsimRandom.reset( config.global().getRandomSeed() );

		try ( final AutocloserModule closer = new AutocloserModule();
				final AutoCloseable monitor = MonitoringUtils.monitorAndLogOnClose() ){
			final SocialNetwork socialNetwork =
					SocialNetworkSamplerUtils.sampleSocialNetwork(
							config,
							closer,
							binder -> binder.bind( CliquesCsvWriter.class ).asEagerSingleton(),
							binder -> binder.bind( SnowballTiesCsvWriter.class ).asEagerSingleton(),
							binder -> binder.bind( EgoCsvWriter.class ).asEagerSingleton(),
							binder -> binder.bind( StopwatchCsvWriter.class ).asEagerSingleton(),
							new SimpleSnowballModule( config ) );

			new SocialNetworkWriter( socialNetwork ).write( config.controler().getOutputDirectory() + "/output_socialNetwork.xml.gz" );
		}
		catch ( RuntimeException e ) {
			throw e;
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}
}

