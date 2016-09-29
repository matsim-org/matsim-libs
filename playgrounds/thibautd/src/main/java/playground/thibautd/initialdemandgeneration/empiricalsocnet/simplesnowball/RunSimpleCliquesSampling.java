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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.simplesnowball;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import playground.ivt.utils.MoreIOUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesCsvWriter;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSamplerUtils;

import java.io.IOException;

import static jdk.nashorn.internal.objects.NativeFunction.bind;
import static org.osgeo.proj4j.parser.Proj4Keyword.a;
import static playground.meisterk.PersonAnalyseTimesByActivityType.Activities.e;

/**
 * @author thibautd
 */
public class RunSimpleCliquesSampling {
	public static void main( String[] args ) {
		final SnowballSamplingConfigGroup configGroup = new SnowballSamplingConfigGroup();
		final Config config = ConfigUtils.loadConfig( args[ 0 ] , configGroup );

		MoreIOUtils.initOut( configGroup.getOutputDirectory() , config );

		try ( final AutocloserModule closer = new AutocloserModule() ){
			final SocialNetwork socialNetwork =
					SocialNetworkSamplerUtils.sampleSocialNetwork(
							config,
							closer,
							binder -> binder.bind( CliquesCsvWriter.class ),
							new SimpleSnowballModule(
									SnowballCliques.readCliques(
											ConfigGroup.getInputFileURL(
													config.getContext(),
													configGroup.getInputCliquesCsv() ).getPath() ) ) );

			new SocialNetworkWriter( socialNetwork ).write( configGroup.getOutputDirectory() + "/output_socialNetwork.xml.gz" );
		}
		catch ( Exception e ) {
			throw new RuntimeException( e );
		}
		finally {
			MoreIOUtils.closeOutputDirLogging();
		}
	}
}

