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

package playground.mohit;

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
		try {
			reader = IOUtils.getBufferedReader(filename);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}

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
				} else if (line.startsWith("$STOPPOINT:")) {
					readStopPoints(line, reader);
				} else if (line.startsWith("$LINE:")) {
					readLines(line, reader);
				} else if (line.startsWith("$LINEROUTE:")) {
					readLineRoutes(line, reader);
				} else if (line.startsWith("$LINEROUTEITEM:")) {
					readLineRouteItems(line, reader);
				} else if (line.startsWith("$TIMEPROFILE:")) {
					readTimeProfile(line, reader);
				} else if (line.startsWith("$TIMEPROFILEITEM:")) {
					readTimeProfileItems(line, reader);
				} else if (line.startsWith("$VEHJOURNEY:")) {
					readDepartures(line, reader);
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
					new CoordImpl(Double.parseDouble(parts[idxXcoord].replace(',', '.')), Double.parseDouble(parts[idxYcoord].replace(',', '.'))));
			this.network.addStop(stop);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readStopPoints(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$STOPPOINT:".length()), ';');
		final int idxNo = getAttributeIndex("NO", attributes);
		final int idxStopNo = getAttributeIndex("STOPAREANO", attributes);
		final int idxName = getAttributeIndex("NAME", attributes);
		final int idxRLNo = getAttributeIndex("LINKNO", attributes);

		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.StopPoint stopPt = new VisumNetwork.StopPoint(new IdImpl(parts[idxNo]), new IdImpl(parts[idxStopNo]),parts[idxName],new IdImpl(parts[idxRLNo]));
			this.network.addStopPoint(stopPt);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLineRoutes(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$LINEROUTE:".length()), ';');
		final int idxName = getAttributeIndex("NAME", attributes);
		final int idxLineName = getAttributeIndex("LINENAME", attributes);
		

		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.TransitLineRoute lr1 = new VisumNetwork.TransitLineRoute(new IdImpl(parts[idxName]), new IdImpl(parts[idxLineName]));
			this.network.addLineRoute(lr1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLines(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$LINE:".length()), ';');
		final int idxName = getAttributeIndex("NAME", attributes);
		final int idxTCode = getAttributeIndex("TSYSCODE", attributes);
		

		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.TransitLine tLine = new VisumNetwork.TransitLine(new IdImpl(parts[idxName]),parts[idxTCode]);
			this.network.addline(tLine);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLineRouteItems(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$LINEROUTEITEM:".length()), ';');
		final int idxLineRouteName = getAttributeIndex("LINEROUTENAME", attributes);
		final int idxLineName = getAttributeIndex("LINENAME", attributes);
		final int idxIndex = getAttributeIndex("INDEX", attributes);
		final int idxStopPointNo = getAttributeIndex("STOPPOINTNO", attributes);
	
	
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.LineRouteItem lri1 = new VisumNetwork.LineRouteItem(parts[idxLineName],parts[idxLineRouteName],parts[idxIndex],new IdImpl(parts[idxStopPointNo]));
			this.network.addLineRouteItem(lri1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readTimeProfile(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$TIMEPROFILE:".length()), ';');
		final int idxLineName = getAttributeIndex("LINENAME", attributes);
		final int idxLineRouteName = getAttributeIndex("LINEROUTENAME", attributes);
		final int idxIndex = getAttributeIndex("NAME", attributes);
		
		
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.TimeProfile tp1 = new VisumNetwork.TimeProfile(new IdImpl(parts[idxLineName]),new IdImpl(parts[idxLineRouteName]),new IdImpl(parts[idxIndex]));
			this.network.addTimeProfile(tp1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readTimeProfileItems(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$TIMEPROFILEITEM:".length()), ';');
		final int idxLineRouteName = getAttributeIndex("LINEROUTENAME", attributes);
		final int idxLineName = getAttributeIndex("LINENAME", attributes);
		final int idxTPName = getAttributeIndex("TIMEPROFILENAME", attributes);
		final int idxIndex = getAttributeIndex("INDEX", attributes);
		final int idxArr = getAttributeIndex("ARR", attributes);
		final int idxDep = getAttributeIndex("DEP", attributes);
		final int idxLRIIndex = getAttributeIndex("LRITEMINDEX", attributes);
		
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.TimeProfileItem tpi1 = new VisumNetwork.TimeProfileItem(parts[idxLineName],parts[idxLineRouteName],parts[idxTPName],parts[idxIndex],parts[idxArr],parts[idxDep],new IdImpl(parts[idxLRIIndex]));
			this.network.addTimeProfileItem(tpi1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readDepartures(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$VEHJOURNEY:".length()), ';');
		final int idxLineRouteName = getAttributeIndex("LINEROUTENAME", attributes);
		final int idxLineName = getAttributeIndex("LINENAME", attributes);
		final int idxIndex = getAttributeIndex("NO", attributes);
		final int idxTRI = getAttributeIndex("TIMEPROFILENAME", attributes);
		final int idxDep = getAttributeIndex("DEP", attributes);
	
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.Departure d = new VisumNetwork.Departure(parts[idxLineName],parts[idxLineRouteName],parts[idxIndex],parts[idxTRI],parts[idxDep]);
			this.network.addDeparture(d);
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
