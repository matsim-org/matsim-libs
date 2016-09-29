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

import com.google.inject.AbstractModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliquesFiller;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.DegreeDistribution;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoLocator;

/**
 * @author thibautd
 */
public class SimpleSnowballModule extends AbstractModule {
	private final SnowballCliques snowballCliques;

	public SimpleSnowballModule( final SnowballCliques snowballCliques ) {
		this.snowballCliques = snowballCliques;
	}

	@Override
	protected void configure() {
		bind( EgoLocator.class ).to( SnowballLocator.class );
		bind( SimpleCliquesFiller.Position.class ).to( SnowballLocator.class );

		bind( DegreeDistribution.class ).to( SimpleDegreeDistribution.class );

		bind( CliquesFiller.class ).to( SimpleCliquesFiller.class );

		bind( SnowballCliques.class ).toInstance( snowballCliques );
	}
}
