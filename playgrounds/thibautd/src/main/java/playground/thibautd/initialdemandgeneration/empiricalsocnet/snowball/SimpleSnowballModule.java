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

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoCharacteristicsDistribution;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoLocator;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.cliquedistributionsnowball.CliqueEgoDistribution;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.cliquedistributionsnowball.CliquesDistributionCliquesFiller;
import playground.thibautd.utils.spatialcollections.SpatialCollectionUtils;

/**
 * @author thibautd
 */
public class SimpleSnowballModule extends AbstractModule {
	private final SnowballCliques snowballCliques;

	// could use matsim module to avoid passing config, but better to read data in constructor (configure might be called
	// several times)
	public SimpleSnowballModule( final Config config ) {
		final SnowballSamplingConfigGroup configGroup =
				(SnowballSamplingConfigGroup) config.getModule(
					SnowballSamplingConfigGroup.GROUP_NAME );
		this.snowballCliques = SnowballCliques.readCliques(
											ConfigGroup.getInputFileURL(
													config.getContext(),
													configGroup.getInputCliquesCsv() ).getPath() );
	}

	@Override
	protected void configure() {
		// this should remain the same between methods
		bind( EgoLocator.class ).to( SnowballLocator.class );
		bind( Position.class ).to( SnowballLocator.class );
		bind( new TypeLiteral<SpatialCollectionUtils.Metric<double[]>>(){} ).to( SnowballLocator.class );

		bind( SocialPositions.class );

		bind( SnowballCliques.class ).toInstance( snowballCliques );

		bind( EgoCharacteristicsDistribution.class ).to( CliqueEgoDistribution.class );
		bind( CliquesFiller.class ).to( CliquesDistributionCliquesFiller.class );
	}
}
