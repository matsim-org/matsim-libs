/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesWriter.java
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
package playground.thibautd.socnetsim.framework.cliques.population;

import static playground.thibautd.socnetsim.framework.cliques.population.CliquesSchemaNames.CLIQUE;
import static playground.thibautd.socnetsim.framework.cliques.population.CliquesSchemaNames.CLIQUES;
import static playground.thibautd.socnetsim.framework.cliques.population.CliquesSchemaNames.CLIQUE_ID;
import static playground.thibautd.socnetsim.framework.cliques.population.CliquesSchemaNames.MEMBER;
import static playground.thibautd.socnetsim.framework.cliques.population.CliquesSchemaNames.MEMBER_ID;

import java.util.Collections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * @author thibautd
 */
public class CliquesWriter extends MatsimXmlWriter {

	public void openAndStartFile(final String fileName) {
		openFile( fileName );
		writeXmlHead();
		writeStartTag(
				CLIQUES,
				Collections.<Tuple<String,String>> emptyList() );
	}

	public void writeClique(
			final Id id,
			final Iterable<? extends Identifiable> members) {
		writeStartTag(
				CLIQUE,
				Collections.singletonList(
					createTuple(
						CLIQUE_ID,
						id.toString() ) ) );

		for ( Identifiable m : members ) {
			writeStartTag(
					MEMBER,
					Collections.singletonList(
						createTuple(
							MEMBER_ID,
							m.getId().toString() ) ),
					true);
		}

		writeEndTag( CLIQUE );
	}

	public void finishAndCloseFile() {
		writeEndTag( CLIQUES );
		close();
	}
}

