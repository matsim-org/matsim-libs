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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.AutocloserModule;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.Ego;
import playground.thibautd.initialdemandgeneration.empiricalsocnet.framework.SocialNetworkSampler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author thibautd
 */
@Singleton
public class EgoCsvWriter implements AutoCloseable {
	private final BufferedWriter writer;

	private final Map<Id<Person>,EgoRecord> records = new HashMap<>();

	@Inject
	public EgoCsvWriter(
			final ControlerConfigGroup controlerConfigGroup,
			final SocialNetworkSampler sampler,
			final AutocloserModule.Closer closer ) {
		this.writer = IOUtils.getBufferedWriter( controlerConfigGroup.getOutputDirectory() +"/output_egos.dat" );

		sampler.addCliqueListener( this::updateRecords );
		closer.add( this );
	}

	private void updateRecords( final Set<Ego> egos ) {
		for ( Ego ego : egos ) {
			final EgoRecord record =
					MapUtils.getArbitraryObject(
							ego.getId(),
							records,
							() -> new EgoRecord( ego ) );

			record.nCliques++;
			// cannot compute triangles, because the same alter can theoretically appear in several cliques
		}
	}

	@Override
	public void close() throws IOException {
		writer.write( "egoId\tnCliques\tnAlters\tplannedDegree\tE_age\tE_sex" );

		for ( EgoRecord record : records.values() ) {
			writer.newLine();
			writer.write( record.ego.getId() +"\t"+ record.nCliques +"\t"+
					record.ego.getAlters().size() +"\t"+ record.ego.getDegree() +"\t"+
					PersonUtils.getAge( record.ego.getPerson() ) +"\t"+
					SocialPositions.getSex( record.ego ) );
		}
		writer.close();
	}

	private static class EgoRecord {
		private final Ego ego;
		int nCliques = 0;

		private EgoRecord( final Ego ego ) {
			this.ego = ego;
		}
	}
}
