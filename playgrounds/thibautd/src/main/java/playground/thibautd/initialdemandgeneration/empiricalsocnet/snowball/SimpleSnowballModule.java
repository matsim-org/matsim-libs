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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoCharacteristicsDistribution;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoLocator;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.cliquedistributionsnowball.CliqueEgoDistribution;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.cliquedistributionsnowball.CliquesDistributionCliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.degreebased.SimpleCliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.degreebased.SimpleEgoDistribution;

/**
 * @author thibautd
 */
public class SimpleSnowballModule extends AbstractModule {
	private final SnowballCliques snowballCliques;
	private final SnowballSamplingConfigGroup configGroup;

	// could use matsim module to avoid passing config, but better to read data in constructor (configure might be called
	// several times)
	public SimpleSnowballModule( final Config config ) {
		this.configGroup = (SnowballSamplingConfigGroup) config.getModule( SnowballSamplingConfigGroup.GROUP_NAME );
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

		bind( SnowballCliques.class ).toInstance( snowballCliques );

		// this varies with method
		switch ( configGroup.getSamplingMethod() ) {
			case degreeBased:
				bind( EgoCharacteristicsDistribution.class ).to( SimpleEgoDistribution.class );
				bind( CliquesFiller.class ).to( SimpleCliquesFiller.class );
				break;
			case cliqueBased:
				bind( EgoCharacteristicsDistribution.class ).to( CliqueEgoDistribution.class );
				bind( CliquesFiller.class ).to( CliquesDistributionCliquesFiller.class );
				if ( true ) throw new RuntimeException( "clique based sampling is bugged. Need to take care of ego Id in SnowballCliques and CliqueEgoDistribution before using" );
				break;
				// TODO
			default:
				throw new RuntimeException( configGroup.getSamplingMethod()+" not implemented yet" );
		}
	}
}
