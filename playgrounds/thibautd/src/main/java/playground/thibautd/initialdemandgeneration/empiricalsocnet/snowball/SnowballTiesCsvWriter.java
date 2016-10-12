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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AbstractCsvWriter;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Writer that writes egos' attributes
 *
 * @author thibautd
 */
@Singleton
public class SnowballTiesCsvWriter extends AbstractCsvWriter {
	@Inject
	public SnowballTiesCsvWriter(
			final ControlerConfigGroup config,
			final SocialNetworkSampler sampler,
			final AutocloserModule.Closer closer ) {
		super( config.getOutputDirectory() +"/output_ties_attr.csv" , sampler , closer );
	}

	@Override
	protected String titleLine() {
		return "egoId\tegoPlannedDegree\tegoAge\tegoSex" +
				"\talterId\talterPlannedDegree\talterAge\talterSex" +
				"\tdistance_m";
	}

	@Override
	protected Iterable<String> cliqueLines( final Set<Ego> clique ) {
		final List<String> lines = new ArrayList<>();

		for ( Ego ego : clique ) {
			for ( Ego alter : clique ) {
				// only write in one direction?
				if ( alter == ego ) continue;

				lines.add( ego.getId() +"\t"+ ego.getDegree() + "\t" +
						PersonUtils.getAge( ego.getPerson() ) + "\t" +
						SocialPositions.getSex( ego ) + "\t" +
						alter.getId() +"\t"+ alter.getDegree() + "\t" +
						PersonUtils.getAge( alter.getPerson() ) + "\t" +
						SocialPositions.getSex( alter ) + "\t" +
						CoordUtils.calcEuclideanDistance(
								SnowballLocator.calcCoord( ego ),
								SnowballLocator.calcCoord( alter ) ) );
			}
		}

		return lines;
	}
}
