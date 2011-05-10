/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.householdsfromcensus;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.core.utils.collections.Tuple;

import org.matsim.core.utils.io.MatsimXmlWriter;

/**
 * Writes clique pertenancy information to an XML file.
 * @author thibautd
 */
public class CliquesWriter extends MatsimXmlWriter {

	private Map<String, ArrayList<String>> cliques;
	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();

	//TODO: change String to ID
	public CliquesWriter(Map<String, ArrayList<String>> cliques) {
		this.cliques = cliques;
	}

	public void writeFile(String fileName) throws FileNotFoundException, IOException {
		this.openFile(fileName);
		this.writeXmlHead();
		this.writeCliques();
		this.close();
	}

	private void writeCliques() throws IOException {
		this.atts.clear();

		//this.atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		this.writeStartTag(CliquesSchemaNames.CLIQUES, this.atts);

		for (String id: this.cliques.keySet()) {
			this.writeClique(id);
		}
		this.writeEndTag(CliquesSchemaNames.CLIQUES);
	}

	private void writeClique(String id) throws IOException {
		this.atts.clear();
		this.atts.add(this.createTuple(CliquesSchemaNames.CLIQUE_ID, id));
		this.writeStartTag(CliquesSchemaNames.CLIQUE, atts);

		this.writeMembers(this.cliques.get(id));

		this.writeEndTag(CliquesSchemaNames.CLIQUE);
	}

	private void writeMembers(ArrayList<String> clique) throws IOException {
		for (String id : clique) {
			this.atts.clear();
			this.atts.add(this.createTuple(CliquesSchemaNames.MEMBER_ID, id));
			this.writeStartTag(CliquesSchemaNames.MEMBER, atts, true);
		}
	}
}

