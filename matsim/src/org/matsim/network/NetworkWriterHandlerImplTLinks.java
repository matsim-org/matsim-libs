/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriterHandlerImplTLinks.java
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

package org.matsim.network;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.misc.Time;

public class NetworkWriterHandlerImplTLinks {

	private final Network network;
	private BufferedWriter out;

	public NetworkWriterHandlerImplTLinks(final Network network) {
		this.network = network;
	}

	public void writeFile(String filename) {
		try {

			this.out = IOUtils.getBufferedWriter(filename);

			startLinks();
			for (Link l : this.network.getLinks().values()) {
				writeLink(l);
			}
			this.out.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private void startLinks() throws IOException {
		this.out.write("ID\t");
		this.out.write("NAME\t");
		this.out.write("NODEA\t");
		this.out.write("NODEB\t");
		this.out.write("PERMLANESA\t");
		this.out.write("PERMLANESB\t");
		this.out.write("LEFTPCKTSA\t");
		this.out.write("LEFTPCKTSB\t");
		this.out.write("RGHTPCKTSA\t");
		this.out.write("RGHTPCKTSB\t");
		this.out.write("TWOWAYTURN\t");
		this.out.write("LENGTH\t");
		this.out.write("GRADE\t");
		this.out.write("SETBACKA\t");
		this.out.write("SETBACKB\t");
		this.out.write("CAPACITYA\t");
		this.out.write("CAPACITYB\t");
		this.out.write("SPEEDLMTA\t");
		this.out.write("SPEEDLMTB\t");
		this.out.write("FREESPDA\t");
		this.out.write("FREESPDB\t");
		this.out.write("FUNCTCLASS\t");
		this.out.write("THRUA\t");
		this.out.write("THRUB\t");
		this.out.write("COLOR\t");
		this.out.write("VEHICLE\t");
		this.out.write("NOTES\n");

	}

	private void writeLink(final Link link) throws IOException {
		
		this.out.write(link.getId() + "\t");			// ID
		this.out.write("[UNKNOWN]\t");					// NAME
		this.out.write(link.getFromNode().getId() + "\t");		// NODEA
		this.out.write(link.getToNode().getId() + "\t");		// NODEB
		this.out.write("0\t");						// PERMLANESA
		this.out.write(link.getLanesAsInt(org.matsim.utils.misc.Time.UNDEFINED_TIME) + "\t");	// PERMLANESB
		this.out.write("0\t");						// LEFTPCKTSA
		this.out.write("0\t");						// LEFTPCKTSB
		this.out.write("0\t");						// RGHTPCKTSA
		this.out.write("0\t");						// RGHTPCKTSB
		this.out.write("0\t");						// TWOWAYTURN
		this.out.write(Math.max(1, (int)link.getLength()) + "\t");		// LENGTH
		this.out.write("0\t");						// GRADE
		this.out.write("0\t");						// SETBACKA
		this.out.write("0\t");						// SETBACKB
		this.out.write("0\t");						// CAPACITYA
		this.out.write((int)link.getCapacity(org.matsim.utils.misc.Time.UNDEFINED_TIME) + "\t");	// CAPACITYB
		this.out.write("0\t");						// SPEEDLMTA
		this.out.write(link.getFreespeed(Time.UNDEFINED_TIME) + "\t");	// SPEEDLMTB
		this.out.write("0\t");						// FREESPDA
		this.out.write(link.getFreespeed(Time.UNDEFINED_TIME) + "\t");	// FREESPDB
		this.out.write("LOCAL\t");					// FUNCTCLASS
		this.out.write("0\t");						// THRUA
		this.out.write("0\t");						// THRUB
		this.out.write("1\t");						// COLOR
		this.out.write("AUTO\t");						// VEHICLE
		this.out.write("\n");							// NOTES
	}

}
