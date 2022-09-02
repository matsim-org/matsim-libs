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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Matrix;

/**
 * @author mrieser
 */
public class VisumMatrixReader {

	private Matrix matrix = null;

	/*package*/ static final Logger log = LogManager.getLogger(VisumMatrixReader.class);

	public VisumMatrixReader(final Matrix matrix) {
		this.matrix = matrix;
	}

	public Matrix readFile(final String filename) {
		BufferedReader infile = null;
		try {
			infile = IOUtils.getBufferedReader(filename);

			this.matrix.setDesc(filename);

			infile.mark(1024);
			String header = infile.readLine();
			infile.reset();

			if (header != null) {
				if (header.equals("$VN;Y5")) {
					new DenseMatrixReader(this.matrix).read(infile);
				} else if (header.startsWith("$O")) {
					new SparseMatrixReader(this.matrix).read(infile);
				} else {
					throw new RuntimeException("Visum file format '" + header +"' is not supported.");
				}
			} else {
				throw new RuntimeException("header is missing");
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (infile != null) {
				try { infile.close(); }
				catch (IOException e) { log.warn("Could not close input-stream.", e); }
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

		private int state = STATE_HEADER;
		private String zoneNames[] = null;
		private int zoneCounter = 0;
		private int lineCounter = 0;
		private int nofZones = 0;
		private final Matrix matrix;

		public DenseMatrixReader(final Matrix matrix) {
			this.matrix = matrix;
		}

		public void read(final BufferedReader in) throws IOException {
			String line = null;
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
					throw new RuntimeException("Expected " + this.nofZones + " data items, but found " + data.length +
					" in line " + this.lineCounter + ".");
				}
				for (int i = 0; i < this.nofZones; i++) {
					this.matrix.setEntry(this.zoneNames[this.zoneCounter], this.zoneNames[i], Double.parseDouble(data[i]));
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
					throw new RuntimeException("Line " + this.lineCounter + ": " +
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

	/*default*/ static class SparseMatrixReader {
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
		private final Matrix matrix;

		public SparseMatrixReader(final Matrix matrix) {
			this.matrix = matrix;
		}

		public void read(final BufferedReader in) throws IOException {
			String line = null;
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
					throw new RuntimeException("Expected 3 tokens, but found " + data.length + " in line " + this.lineCounter + "."
					);
				}

				String from = data[0];
				String to = data[1];

				/*
				 * There is no intrazonal traffic in VISUM.
				 * However, when there comes a matrix entry from VISUM 
				 * for a tuple of one and the same zone, store NaN instead of the value.
				 * This indicates that the value has to be computed otherwise in MATSim.
				 */
				double value = Double.NaN;
				if (!from.equals(to)) {
					value = Double.parseDouble(data[2]);
				}
				this.matrix.createAndAddEntry(from, to, value);

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
				throw new RuntimeException("unknown internal state: " + this.state);
			}
		}

	}

}
