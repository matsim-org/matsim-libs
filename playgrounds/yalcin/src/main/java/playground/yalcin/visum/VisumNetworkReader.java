/* *********************************************************************** *
 * project: org.matsim.*
 * VisumNetReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.yalcin.visum;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

public class VisumNetworkReader {

	private final VisumNetwork network;

	private final Logger log = Logger.getLogger(VisumNetworkReader.class);

	public VisumNetworkReader(final VisumNetwork network) {
		this.network = network;
	}

	public void read(final String filename) throws FileNotFoundException, IOException {
		BufferedReader reader;
		reader = IOUtils.getBufferedReader(filename);

		try {
			String line = reader.readLine();
			if (!"$VISION".equals(line)) {
				throw new IOException("File does not start with '$VISION'. Are you sure it is a VISUM network file?");
			}
			// next line after header:
			line = reader.readLine();
			while (line != null) {
				if (line.startsWith("* ")) {
					// just a comment, ignore it
				} else if (line.startsWith("$STOP:")) {
					readStops(line, reader);
				} else if (line.startsWith("$")) {
					readUnknownTable(line, reader);
				} else {
					throw new IOException("can not interpret line: " + line);
				}
				// next line:
				line = reader.readLine();
			}

		} catch (IOException e) {
			this.log.warn("there was an exception while reading the file.", e);
			try {
				reader.close();
			} catch (IOException e2) {
				this.log.warn("could not close reader.", e2);
			}
			throw e;
		}

		try {
			reader.close();
		} catch (IOException e) {
			this.log.warn("could not close reader.", e);
		}

	}

	private void readStops(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$STOP:".length()), ';');
		final int idxNo = getAttributeIndex("NO", attributes);
		final int idxName = getAttributeIndex("NAME", attributes);
		final int idxXcoord = getAttributeIndex("XCOORD", attributes);
		final int idxYcoord = getAttributeIndex("YCOORD", attributes);

		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.Stop stop = new VisumNetwork.Stop(new IdImpl(parts[idxNo]), parts[idxName],
					new CoordImpl(Double.parseDouble(parts[idxXcoord]), Double.parseDouble(parts[idxYcoord])));
			this.network.addStop(stop);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readUnknownTable(final String tableAttributes, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line.length() > 0) {

			line = reader.readLine();
		}
 	}

	private int getAttributeIndex(final String attribute, final String[] attributes) {
		for (int i = 0; i < attributes.length; i++) {
			if (attributes[i].equals(attribute)) {
				return i;
			}
		}
		return -1;
	}
}
