/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationAreaFileWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.sims.evacbase;

import java.io.IOException;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;

/**
 *@author glaemmel
 */
public class EvacuationAreaFileWriter extends MatsimXmlWriter {

	private final Map<Id, EvacuationAreaLink> links;

	public EvacuationAreaFileWriter(final Map<Id, EvacuationAreaLink> el) {
		this.links = el;
	}

	public void writeFile(final String filename) throws IOException {
		openFile(filename);
		writeXmlHead();
		writeDoctype("evacuationarea", DEFAULT_DTD_LOCATION + "evacuationarea_v1.dtd");
		write();
		close();
	}

	private void write() throws IOException {
		this.writer.write("<evacuationarea>" + NL);
		writeBody("\t");
		this.writer.write("</evacuationarea>" + NL);
	}

	private void writeBody(String indent) throws IOException {
		for (EvacuationAreaLink link : this.links.values()) {
			this.writer.write(indent +"<link id=\"" + link.getId() + "\" deadline=\""
					+ Time.writeTime(link.getDeadline()) + "\" />\n");
		}
	}

}
