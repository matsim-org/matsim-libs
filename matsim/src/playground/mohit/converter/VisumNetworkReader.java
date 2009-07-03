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

package playground.mohit.converter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import playground.mohit.converter.VisumNetwork;


public class VisumNetworkReader {

	private final VisumNetwork network;

	private final Logger log = Logger.getLogger(VisumNetworkReader.class);

	/** index for accessing the localized strings. */
	private int language = 0;
	
	/* collection of localized strings: [0] english, [1] german */
	private final String[] TABLE_STOP = {"$STOP:", "$HALTESTELLE:"};
	private final String[] TABLE_STOPAREA = {"$STOPAREA:", "$HALTESTELLENBEREICH:"};
	private final String[] TABLE_STOPPOINT = {"$STOPPOINT:", "$HALTEPUNKT:"};
	private final String[] TABLE_LINE = {"$LINE:", "$LINIE:"};
	private final String[] TABLE_LINEROUTE = {"$LINEROUTE:", "$LINIENROUTE:"};
	private final String[] TABLE_LINEROUTEITEM = {"$LINEROUTEITEM:", "$LINIENROUTENELEMENT:"};
	private final String[] TABLE_TIMEPROFILE = {"$TIMEPROFILE:", "$FAHRZEITPROFIL:"};
	private final String[] TABLE_TIMEPROFILEITEM = {"$TIMEPROFILEITEM:", "$FAHRZEITPROFILELEMENT:"};
	private final String[] TABLE_VEHJOURNEY = {"$VEHJOURNEY:", "$FZGFAHRT:"};
	
	private final String[] ATTRIBUTE_STOP_NO = {"NO", "NR"};
	private final String[] ATTRIBUTE_STOP_NAME = {"NAME", "NAME"};
	private final String[] ATTRIBUTE_STOP_XCOORD = {"XCOORD", "XKOORD"};
	private final String[] ATTRIBUTE_STOP_YCOORD = {"YCOORD", "YKOORD"};

	private final String[] ATTRIBUTE_STOPAREA_NO = {"NO", "NR"};
	private final String[] ATTRIBUTE_STOPAREA_STOPNO = {"STOPNO", "HSTNR"};
	
	private final String[] ATTRIBUTE_STOPPT_NO = {"NO", "NR"};
	private final String[] ATTRIBUTE_STOPPT_STOPAREANO = {"STOPAREANO", "HSTBERNR"};
	private final String[] ATTRIBUTE_STOPPT_NAME =  {"NAME", "NAME"};
	private final String[] ATTRIBUTE_STOPPT_RLNO = {"LINKNO", "STRNR"};
	
	private final String[] ATTRIBUTE_LR_NAME = {"NAME", "NAME"};
	private final String[] ATTRIBUTE_LR_LINENAME = {"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_LR_DCODE = {"DIRECTIONCODE", "RICHTUNGCODE"};
	
	private final String[] ATTRIBUTE_L_NAME = {"NAME", "NAME"};
	private final String[] ATTRIBUTE_L_TCODE = {"TSYSCODE", "VSYSCODE"};
	
	private final String[] ATTRIBUTE_LRI_LRNAME = {"LINEROUTENAME", "LINROUTENAME"};
	private final String[] ATTRIBUTE_LRI_LNAME ={"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_LRI_ID ={"INDEX", "INDEX"};
	private final String[] ATTRIBUTE_LRI_DCODE ={"DIRECTIONCODE", "RICHTUNGCODE"};
	private final String[] ATTRIBUTE_LRI_SPNO = {"STOPPOINTNO", "HPUNKTNR"};
	
	private final String[] ATTRIBUTE_TP_LNAME ={"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_TP_LRNAME = {"LINEROUTENAME", "LINROUTENAME"};
	private final String[] ATTRIBUTE_TP_ID ={"NAME", "NAME"};	
	private final String[] ATTRIBUTE_TP_DCODE ={"DIRECTIONCODE", "RICHTUNGCODE"};
	
	private final String[] ATTRIBUTE_TPI_LNAME ={"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_TPI_LRNAME = {"LINEROUTENAME", "LINROUTENAME"};
	private final String[] ATTRIBUTE_TPI_ID ={"INDEX", "INDEX"};	
	private final String[] ATTRIBUTE_TPI_TPNAME ={"TIMEPROFILENAME", "FZPROFILNAME"};
	private final String[] ATTRIBUTE_TPI_DCODE ={"DIRECTIONCODE", "RICHTUNGCODE"};
	private final String[] ATTRIBUTE_TPI_ARR ={"ARR", "ANKUNFT"};
	private final String[] ATTRIBUTE_TPI_DEP ={"DEP", "ABFAHRT"};
	private final String[] ATTRIBUTE_TPI_LRIINDEX ={"LRITEMINDEX", "LRELEMINDEX"};
	
	private final String[] ATTRIBUTE_D_LNAME ={"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_D_LRNAME = {"LINEROUTENAME", "LINROUTENAME"};
	private final String[] ATTRIBUTE_D_ID ={"NO", "NR"};	
	private final String[] ATTRIBUTE_D_TPNAME ={"TIMEPROFILENAME", "FZPROFILNAME"};	
	private final String[] ATTRIBUTE_D_DEP ={"DEP", "ABFAHRT"};	
	private final String[] ATTRIBUTE_D_DCODE ={"DIRECTIONCODE", "RICHTUNGCODE"};	
	
	
	
	
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
				} else if (line.startsWith("$VERSION:")) {
					readVersion(line, reader);
				} else if (line.startsWith(TABLE_STOP[language])) {
					readStops(line, reader);
				} else if (line.startsWith(TABLE_STOPAREA[language])) {
					readStopAreas(line, reader);
				} else if (line.startsWith(TABLE_STOPPOINT[language])) {
					readStopPoints(line, reader);
				} else if (line.startsWith(TABLE_LINE[language])) {
					readLines(line, reader);
				} else if (line.startsWith(TABLE_LINEROUTE[language])) {
					readLineRoutes(line, reader);
				} else if (line.startsWith(TABLE_LINEROUTEITEM[language])) {
					readLineRouteItems(line, reader);
				} else if (line.startsWith(TABLE_TIMEPROFILE[language])) {
					readTimeProfile(line, reader);
				} else if (line.startsWith(TABLE_TIMEPROFILEITEM[language])) {
					readTimeProfileItems(line, reader);
				} else if (line.startsWith(TABLE_VEHJOURNEY[language])) {
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
	

	private void readVersion(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring("$VERSION:".length()), ';');
		final int idxLanguage = getAttributeIndex("LANGUAGE", attributes);
		
		String line = reader.readLine();
		final String[] parts = StringUtils.explode(line, ';');
		if (parts[idxLanguage].equals("ENG")) {
			this.language = 0;
		} else if (parts[idxLanguage].equals("DEU")) {
			this.language = 1;
		} else {
			throw new RuntimeException("Unknown language: " + parts[idxLanguage]);
		}
		// proceed to next line, should be empty
		line = reader.readLine();
	}

	private void readStops(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_STOP[language].length()), ';');
		final int idxNo = getAttributeIndex(ATTRIBUTE_STOP_NO[language], attributes);
		final int idxName = getAttributeIndex(ATTRIBUTE_STOP_NAME[language], attributes);
		final int idxXcoord = getAttributeIndex(ATTRIBUTE_STOP_XCOORD[language], attributes);
		final int idxYcoord = getAttributeIndex(ATTRIBUTE_STOP_YCOORD[language], attributes);

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
	
	private void readStopAreas(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_STOPAREA[language].length()), ';');
		final int idxNo = getAttributeIndex(ATTRIBUTE_STOPAREA_NO[language], attributes);
		final int idxStopId = getAttributeIndex(ATTRIBUTE_STOPAREA_STOPNO[language], attributes);

		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.StopArea stopAr = new VisumNetwork.StopArea(new IdImpl(parts[idxNo]), new IdImpl(parts[idxStopId]));
			this.network.addStopArea(stopAr);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readStopPoints(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring( TABLE_STOPPOINT[language].length()), ';');
		final int idxNo = getAttributeIndex(ATTRIBUTE_STOPPT_NO[language], attributes);
		final int idxStopAreaNo = getAttributeIndex(ATTRIBUTE_STOPPT_STOPAREANO[language], attributes);
		final int idxName = getAttributeIndex(ATTRIBUTE_STOPPT_NAME[language], attributes);
		final int idxRLNo = getAttributeIndex(ATTRIBUTE_STOPPT_RLNO[language], attributes);

		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.StopPoint stopPt = new VisumNetwork.StopPoint(new IdImpl(parts[idxNo]), new IdImpl(parts[idxStopAreaNo]),parts[idxName],new IdImpl(parts[idxRLNo]));
			this.network.addStopPoint(stopPt);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLineRoutes(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_LINEROUTE[language].length()), ';');
		final int idxName = getAttributeIndex(ATTRIBUTE_LR_NAME[language], attributes);
		final int idxLineName = getAttributeIndex(ATTRIBUTE_LR_LINENAME[language], attributes);
		final int idxDCode = getAttributeIndex(ATTRIBUTE_LR_DCODE[language], attributes);

		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.TransitLineRoute lr1 = new VisumNetwork.TransitLineRoute(new IdImpl(parts[idxName]), new IdImpl(parts[idxLineName]),new IdImpl(parts[idxDCode]));
			this.network.addLineRoute(lr1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLines(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_LINE[language].length()), ';');
		final int idxName = getAttributeIndex(ATTRIBUTE_L_NAME[language], attributes);
		final int idxTCode = getAttributeIndex(ATTRIBUTE_L_TCODE[language], attributes);
		

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
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_LINEROUTEITEM[language].length()), ';');
		final int idxLineRouteName = getAttributeIndex(ATTRIBUTE_LRI_LRNAME[language], attributes);
		final int idxLineName = getAttributeIndex(ATTRIBUTE_LRI_LNAME[language], attributes);
		final int idxIndex = getAttributeIndex(ATTRIBUTE_LRI_ID[language], attributes);
		final int idxDCode = getAttributeIndex(ATTRIBUTE_LRI_DCODE[language], attributes);
		final int idxStopPointNo = getAttributeIndex(ATTRIBUTE_LRI_SPNO[language], attributes);
	
	
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.LineRouteItem lri1 = new VisumNetwork.LineRouteItem(parts[idxLineName],parts[idxLineRouteName],parts[idxIndex],parts[idxDCode],new IdImpl(parts[idxStopPointNo]));
			this.network.addLineRouteItem(lri1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readTimeProfile(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_TIMEPROFILE[language].length()), ';');
		final int idxLineName = getAttributeIndex(ATTRIBUTE_TP_LNAME[language], attributes);
		final int idxLineRouteName = getAttributeIndex(ATTRIBUTE_TP_LRNAME[language], attributes);
		final int idxIndex = getAttributeIndex(ATTRIBUTE_TP_ID[language], attributes);
		final int idxDCode = getAttributeIndex(ATTRIBUTE_TP_DCODE[language], attributes);
		
		
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.TimeProfile tp1 = new VisumNetwork.TimeProfile(new IdImpl(parts[idxLineName]),new IdImpl(parts[idxLineRouteName]),new IdImpl(parts[idxIndex]),new IdImpl(parts[idxDCode]));
			this.network.addTimeProfile(tp1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readTimeProfileItems(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_TIMEPROFILEITEM[language].length()), ';');
		final int idxLineRouteName = getAttributeIndex(ATTRIBUTE_TPI_LRNAME[language], attributes);
		final int idxLineName = getAttributeIndex(ATTRIBUTE_TPI_LNAME[language], attributes);
		final int idxTPName = getAttributeIndex(ATTRIBUTE_TPI_TPNAME[language], attributes);
		final int idxDCode = getAttributeIndex(ATTRIBUTE_TPI_DCODE[language], attributes);
		final int idxIndex = getAttributeIndex(ATTRIBUTE_TPI_ID[language], attributes);
		final int idxArr = getAttributeIndex(ATTRIBUTE_TPI_ARR[language], attributes);
		final int idxDep = getAttributeIndex(ATTRIBUTE_TPI_DEP[language], attributes);
		final int idxLRIIndex = getAttributeIndex(ATTRIBUTE_TPI_LRIINDEX[language], attributes);
		
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.TimeProfileItem tpi1 = new VisumNetwork.TimeProfileItem(parts[idxLineName],parts[idxLineRouteName],parts[idxTPName],parts[idxDCode],parts[idxIndex],parts[idxArr],parts[idxDep],new IdImpl(parts[idxLRIIndex]));
			this.network.addTimeProfileItem(tpi1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readDepartures(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(TABLE_VEHJOURNEY[language].length()), ';');
		final int idxLineRouteName = getAttributeIndex(ATTRIBUTE_D_LRNAME[language], attributes);
		final int idxLineName = getAttributeIndex(ATTRIBUTE_D_LNAME[language], attributes);
		final int idxIndex = getAttributeIndex(ATTRIBUTE_D_ID[language], attributes);
		final int idxTRI = getAttributeIndex(ATTRIBUTE_D_TPNAME[language], attributes);
		final int idxDep = getAttributeIndex(ATTRIBUTE_D_DEP[language], attributes);
		final int idxDCode = getAttributeIndex(ATTRIBUTE_D_DCODE[language], attributes);
		
		String line = reader.readLine();
		while (line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
		
			VisumNetwork.Departure d = new VisumNetwork.Departure(parts[idxLineName],parts[idxLineRouteName],parts[idxIndex],parts[idxTRI],parts[idxDep],new IdImpl(parts[idxDCode]));
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
