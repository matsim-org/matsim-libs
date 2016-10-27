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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.cliquedistributionsnowball;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.CliqueStub;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.EgoCharacteristicsDistribution;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.snowball.SnowballCliques;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
@Singleton
public class CliqueEgoDistribution implements EgoCharacteristicsDistribution {
	private final Random random = MatsimRandom.getLocalInstance();

	// could be compressed, by storing (cumulative) counts in another array and searching with binary search on count
	private final CliqueStructure[] cliqueStructures;

	@Inject
	public CliqueEgoDistribution( final SnowballCliques cliques ) {
		final Map<SnowballCliques.Member,List<SnowballCliques.Clique>> cliquesPerEgo = new LinkedHashMap<>();
		cliques.getCliques().values().forEach( c -> MapUtils.getList( c.getEgo() , cliquesPerEgo ).add( c ) );

		this.cliqueStructures = cliquesPerEgo.entrySet().stream()
				.map( e -> new CliqueStructure( e.getKey().getDegree() , e.getValue() ) )
				.collect( Collectors.toList() )
				.toArray( new CliqueStructure[ cliquesPerEgo.size() ]);
	}

	@Override
	public Tuple<Ego,Collection<CliqueStub>> sampleEgo( final Person person ) {
		final CliqueStructure structure = cliqueStructures[ random.nextInt( cliqueStructures.length ) ];

		final Ego ego = new Ego( person , structure.degree );
		return new Tuple<>(
				ego,
				Arrays.stream( structure.sizes )
						.mapToObj( s -> new CliqueStub( s, ego ) )
						.collect( Collectors.toList() ) );
	}

	private static class CliqueStructure {
		private final int degree;
		private final int[] sizes;

		public CliqueStructure(
				final int degree,
				final List<SnowballCliques.Clique> cliques ) {
			this.degree = degree;
			this.sizes = cliques.stream()
					.mapToInt( c -> c.getAlters().size() + 1 )
					.toArray();
			Arrays.sort( sizes );
		}
	}
}
