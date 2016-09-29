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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.core.config.groups.ControlerConfigGroup;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
@Singleton
public class CliquesCsvWriter extends AbstractCsvWriter {
	private int cliqueId = 0;

	@Inject
	public CliquesCsvWriter(
			final ControlerConfigGroup config,
			final SocialNetworkSampler sampler,
			final AutocloserModule.Closer closer ) {
		super( config.getOutputDirectory() +"/output_cliques.csv" , sampler , closer );
	}

	@Override
	protected String titleLine() {
		return "cliqueId\tegoId";
	}

	@Override
	protected Iterable<String> cliqueLines( final Set<Ego> clique ) {
		final int currentClique = cliqueId++;
		return clique.stream()
				.map( ego -> currentClique +"\t"+ ego.getId() )
				.collect( Collectors.toList() );
	}
}
