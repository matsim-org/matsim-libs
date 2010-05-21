/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.iavis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

public class IANetworkWriter {

	private final static Logger log = Logger.getLogger(IANetworkWriter.class);

	public void write(Network network, String filename) throws FileNotFoundException, IOException {
		BufferedWriter writer = null;
		try {
			writer = IOUtils.getBufferedWriter(filename);
			writer.write("Id\t");
			writer.write("fromX\t");
			writer.write("fromY\t");
			writer.write("toX\t");
			writer.write("toY\n");

			for (Link link : network.getLinks().values()) {
				writer.write(link.getId().toString() + "\t");
				writer.write(link.getFromNode().getCoord().getX() + "\t");
				writer.write(link.getFromNode().getCoord().getY() + "\t");
				writer.write(link.getToNode().getCoord().getX() + "\t");
				writer.write(link.getToNode().getCoord().getY() + "\n");
			}

		} finally {
			if (writer != null) {
				try { writer.close(); }
				catch (IOException e) { log.warn("Could not close writer.", e); }
			}
		}
	}

}
