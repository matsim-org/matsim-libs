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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.toy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliqueStub;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoCharacteristicsDistribution;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author thibautd
 */
@Singleton
public class ToyEgoDistribution implements EgoCharacteristicsDistribution {
	private final ToySocialNetworkConfigGroup configGroup;

	@Inject
	public ToyEgoDistribution( final ToySocialNetworkConfigGroup configGroup ) {
		this.configGroup = configGroup;
	}

	@Override
	public Tuple<Ego, Collection<CliqueStub>> sampleEgo( final Person person ) {
		final int nCliques = configGroup.getNumberOfCliques();

		final Ego ego = new Ego( person , nCliques * configGroup.getCliqueSize() );
		final Collection<CliqueStub> stubs = new ArrayList<>( nCliques );
		for ( int i = 0; i < nCliques; i++ ) {
			stubs.add( new CliqueStub( configGroup.getCliqueSize() , ego ) );
		}

		return new Tuple<>( ego , stubs );
	}
}
