/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationNetFileWriter.java
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

package playground.gregor.evacuation.plansgeneration;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;

import org.matsim.gbl.Gbl;
import org.matsim.utils.identifiers.IdI;

import playground.gregor.evacuation.EvacuationAreaLink;



/**
 * @author glaemmel
 *
 */

//////////////////////////////////////////////////////////////////////
//EvacuationNetFileWriter writes the desaster area links to xml
//////////////////////////////////////////////////////////////////////
public class EvacuationNetFileWriter extends MatsimXmlWriter {




	HashMap<IdI, EvacuationAreaLink> links;

	public EvacuationNetFileWriter(HashMap<IdI, EvacuationAreaLink> links) {

			this.links = links;
	}


	public void writeFile(String filename) {
		try {
			openFile(filename);
			writeXmlHead();
			write();
			close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void writeStream(Writer writer) {
		try {
			this.writer = writer;
			write();
			this.writer.flush();
			this.writer = null;
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void write() throws IOException {
		this.writer.write("<!DOCTYPE evacuationarea SYSTEM \"evacuationarea_v1.dtd\">");
		this.writer.write("<evacuationarea>" + NL);
		writeBody("\t");
		this.writer.write("</evacuationarea>" + NL);
	}

	private void writeBody(String indent) throws IOException {
		Iterator it = links.values().iterator();
		while (it.hasNext()){
			EvacuationAreaLink link = (EvacuationAreaLink) it.next();
			this.writer.write(indent +"<link id=\"" + link.getId() + "\" deadline=\""
					+ Gbl.writeTime(link.getDeadline()) + "\" />\n");
		}

	}



}






