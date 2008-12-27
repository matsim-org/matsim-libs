/* *********************************************************************** *
 * project: org.matsim.*
 * VisumMatrixReader.java
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

package org.matsim.visum;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.Layer;
import org.matsim.world.Location;

/**
 * @author marcel
 *
 */
public class VisumMatrixReader {

	/*default*/ Layer layer = null;
	private String id = null;

	/*default*/ static final Logger log = Logger.getLogger(VisumMatrixReader.class);

	private final ArrayList<String> knownMissingLocations = new ArrayList<String>();

	public VisumMatrixReader(final String id, final Layer layer) {
		this.id = id;
		this.layer = layer;
	}

	public Matrix readFile(final String filename) {
		Matrix matrix = null;
		matrix = Matrices.getSingleton().createMatrix(this.id, this.layer, "");
		BufferedReader infile = null;
		try {
			infile = IOUtils.getBufferedReader(filename);

			matrix.setDesc(filename);

			infile.mark(1024);
			String header = infile.readLine();
			infile.reset();

			if (header != null) {
				if (header.equals("$VN;Y5")) {
					new DenseMatrixReader(this.layer).read(infile, matrix);
				} else if (header.startsWith("$O")) {
					new SparseMatrixReader().read(infile, matrix);
				} else {
					Gbl.errorMsg("Visum file format '" + header +"' is not supported.");
				}
			} else {
				Gbl.errorMsg("header is missing");
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (infile != null) {
				try { infile.close(); } catch (IOException ignored) {}
			}
		}

		return matrix;
	}

	private static class DenseMatrixReader {
		/* I call the format, where the matrix is stored as a table in the file,
		 * a "dense matrix" format, as it is most effective for dense matrices.  */
		private final static int STATE_HEADER = 0;
		private final static int STATE_ANZBEZIRKE = 1;
		private final static int STATE_BEZIRKE = 2;
		private final static int STATE_DATA = 3;
		private final static int STATE_GARBAGE = 4;

		private final Layer layer;
		private int state = STATE_HEADER;
		private String zoneNames[] = null;
		private int zoneCounter = 0;
		private int lineCounter = 0;
		private int nofZones = 0;
		private Matrix matrix = null;

		public DenseMatrixReader(final Layer layer) {
			this.layer = layer;
		}

		public void read(final BufferedReader in, final Matrix matrix) throws IOException {
			String line = null;
			this.matrix = matrix;
			while ( (line = in.readLine()) != null) {
				this.lineCounter++;
				if (!line.startsWith("*")) {
					parseLine(line);
				}
			}
		}

		private void parseLine(final String line) {

			if (this.state == STATE_DATA) {

				String[] data = line.split("\t");
				if (data.length != this.nofZones) {
					Gbl.errorMsg("Expected " + this.nofZones + " data items, but found " + data.length +
					" in line " + this.lineCounter + ".");
				}
				for (int i = 0; i < this.nofZones; i++) {
					this.matrix.setEntry(this.layer.getLocation(this.zoneNames[this.zoneCounter]),
							this.layer.getLocation(this.zoneNames[i]), Double.parseDouble(data[i]));
				}
				this.zoneCounter++;
				if (this.zoneCounter == this.nofZones) {
					this.state = STATE_GARBAGE;
				}

			} else if (this.state == STATE_HEADER) {

				// we ignore the header line
				this.state = STATE_ANZBEZIRKE;	// we expect as next the number of zones

			} else if (this.state == STATE_ANZBEZIRKE) {

				this.nofZones = Integer.parseInt(line);
				this.state = STATE_BEZIRKE;

			} else if (this.state == STATE_BEZIRKE) {

				this.zoneNames = line.split("\t");
				if (this.zoneNames.length != this.nofZones) {
					Gbl.errorMsg("Line " + this.lineCounter + ": " +
							"The actual number of zones (" + this.zoneNames.length + ") does not " +
							"correspond with the expected number of zones (" + this.nofZones + ")."
					);
				}
				this.zoneCounter = 0;
				this.state = STATE_DATA;

//			} else if (this.state == STATE_GARBAGE) {
//				 ignore the data
			}
		}

	}

	/*default*/ class SparseMatrixReader {
		/* I call the format, where each entry of the matrix is written on its
		 * own line together with a from- and to-cell-id, a "sparse matrix" format,
		 * as it is most effective for storing sparse matrices.   */
		private final static int STATE_HEADER = 0;
		private final static int STATE_VERKEHRSMITTEL = 1;
		private final static int STATE_TIME = 2;
		private final static int STATE_FAKTOR = 3;
		private final static int STATE_DATA = 4;

		private int state = STATE_HEADER;
		private int lineCounter = 0;
		private Matrix matrix = null;

		public void read(final BufferedReader in, final Matrix matrix) throws IOException {
			String line = null;
			this.matrix = matrix;
			while ( (line = in.readLine()) != null) {
				this.lineCounter++;
				if (!line.startsWith("*")) {
					parseLine(line);
				}
			}
		}

		private void parseLine(final String line) {

			if (this.state == STATE_DATA) {

				String[] data = line.trim().split("\\s+");
				if (data.length != 3) {
					Gbl.errorMsg("Expected 3 tokens, but found " + data.length +
					" in line " + this.lineCounter + "."
					);
				}

				Location from = VisumMatrixReader.this.layer.getLocation(data[0]);
				Location to = VisumMatrixReader.this.layer.getLocation(data[1]);

				if (from == null) {
					warnMissingLocation(data[0]);
					return;
				}
				if (to == null) {
					warnMissingLocation(data[1]);
					return;
				}
				this.matrix.createEntry(from, to, Double.parseDouble(data[2]));

			} else if (this.state == STATE_HEADER) {

				// we ignore the header line
				this.state = STATE_VERKEHRSMITTEL;	// we expect as next the Verkehrsmittel-Nr

			} else if (this.state == STATE_VERKEHRSMITTEL) {

				// ignore verkehrsmittel
				this.state = STATE_TIME;

			} else if (this.state == STATE_TIME) {

				// ignore time
				this.state = STATE_FAKTOR;

			} else if (this.state == STATE_FAKTOR) {

				// ignore faktor
				this.state = STATE_DATA;

			} else {
				Gbl.errorMsg("unknown internal state: " + this.state);
			}
		}

	}

	/*default*/ void warnMissingLocation(final String locName) {
		if (!this.knownMissingLocations.contains(locName)) {
			this.knownMissingLocations.add(locName);
			log.warn("Location " + locName + " does not exist in world.");
		}
	}

}
