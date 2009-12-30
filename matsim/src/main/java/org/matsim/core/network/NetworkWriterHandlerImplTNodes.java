/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkWriterHandlerImplTNodes.java
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

package org.matsim.core.network;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.api.internal.MatsimFileWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

public class NetworkWriterHandlerImplTNodes implements MatsimFileWriter {

	private final NetworkLayer network;

	public NetworkWriterHandlerImplTNodes(final NetworkLayer network) {
		this.network = network;
	}

	public void writeFile(String filename) {
		try {

			BufferedWriter out = IOUtils.getBufferedWriter(filename);

			startNodes(out);
			for (NodeImpl n : this.network.getNodes().values()) {
				writeNode(n, out);
			}
			out.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}

	private void startNodes(final BufferedWriter out) throws IOException {
		out.write("ID\t");
		out.write("EASTING\t");
		out.write("NORTHING\t");
		out.write("ELEVATION\t");
		out.write("NOTES\n");
	}

	private void writeNode(final NodeImpl node, final BufferedWriter out) throws IOException {

		out.write(node.getId() + "\t");
		out.write(node.getCoord().getX() + "\t");
		out.write(node.getCoord().getY() + "\t");
		out.write("0\t");	// elevation
		out.write("\n");		// notes
	}
}
