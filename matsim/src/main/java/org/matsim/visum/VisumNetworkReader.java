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

package org.matsim.visum;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;


public class VisumNetworkReader {

	private static final String ATTRIBUTE_UNKNOWN = "%%%KEINE_AHNUNG%%%";

	private final VisumNetwork network;

	private final Logger log = Logger.getLogger(VisumNetworkReader.class);

	/** index for accessing the localized strings. */
	private int language = 0;

	/* collection of localized strings: [0] english, [1] german */
	private final String[] TABLE_NODE = {ATTRIBUTE_UNKNOWN, "$KNOTEN:"};
	private final String[] TABLE_EDGE = {ATTRIBUTE_UNKNOWN, "$STRECKE:"};
	private final String[] TABLE_EDGETYPE = {ATTRIBUTE_UNKNOWN, "$STRECKENTYP:"};
	private final String[] TABLE_STOP = {"$STOP:", "$HALTESTELLE:"};
	private final String[] TABLE_STOPAREA = {"$STOPAREA:", "$HALTESTELLENBEREICH:"};
	private final String[] TABLE_STOPPOINT = {"$STOPPOINT:", "$HALTEPUNKT:"};
	private final String[] TABLE_LINE = {"$LINE:", "$LINIE:"};
	private final String[] TABLE_LINEROUTE = {"$LINEROUTE:", "$LINIENROUTE:"};
	private final String[] TABLE_LINEROUTEITEM = {"$LINEROUTEITEM:", "$LINIENROUTENELEMENT:"};
	private final String[] TABLE_TIMEPROFILE = {"$TIMEPROFILE:", "$FAHRZEITPROFIL:"};
	private final String[] TABLE_TIMEPROFILEITEM = {"$TIMEPROFILEITEM:", "$FAHRZEITPROFILELEMENT:"};
	private final String[] TABLE_VEHJOURNEY = {"$VEHJOURNEY:", "$FZGFAHRT:"};
	private final String[] TABLE_VEHUNIT = {"$VEHUNIT:", "$FZGEINHEIT:"};
	private final String[] TABLE_VEHCOMB = {"$VEHCOMB:", "$FZGKOMB:"};
	private final String[] TABLE_VEHUNITTOVEHCOMB = {"$VEHUNITTOVEHCOMB:", "$FZGEINHEITZUFZGKOMB:"};

	private final String[] ATTRIBUTE_NODE_NO = {ATTRIBUTE_UNKNOWN, "NR"};
	private final String[] ATTRIBUTE_NODE_NAME = {ATTRIBUTE_UNKNOWN, "NAME"};
	private final String[] ATTRIBUTE_NODE_XCOORD = {ATTRIBUTE_UNKNOWN, "XKOORD"};
	private final String[] ATTRIBUTE_NODE_YCOORD = {ATTRIBUTE_UNKNOWN, "YKOORD"};

	private final String[] ATTRIBUTE_EDGE_NO = {ATTRIBUTE_UNKNOWN, "NR"};
	private final String[] ATTRIBUTE_EDGE_FROM_NODE = {ATTRIBUTE_UNKNOWN, "VONKNOTNR"};
	private final String[] ATTRIBUTE_EDGE_TO_NODE = {ATTRIBUTE_UNKNOWN, "NACHKNOTNR"};
	private final String[] ATTRIBUTE_EDGE_LENGTH = {ATTRIBUTE_UNKNOWN, "LAENGE"};
	private final String[] ATTRIBUTE_EDGE_EDGETYPEID = {ATTRIBUTE_UNKNOWN, "TYPNR"};
	
	private final String[] ATTRIBUTE_EDGETYPE_NO = {ATTRIBUTE_UNKNOWN, "NR"};
	private final String[] ATTRIBUTE_EDGETYPE_KAPIV = {ATTRIBUTE_UNKNOWN, "KAPIV"};
	private final String[] ATTRIBUTE_EDGETYPE_V0IV = {ATTRIBUTE_UNKNOWN, "V0IV"};
	
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
	private final String[] ATTRIBUTE_STOPPT_NODE = {ATTRIBUTE_UNKNOWN, "KNOTNR"};

	private final String[] ATTRIBUTE_LR_NAME = {"NAME", "NAME"};
	private final String[] ATTRIBUTE_LR_LINENAME = {"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_LR_DCODE = {"DIRECTIONCODE", "RICHTUNGCODE"};
	private final String[] ATTRIBUTE_LR_TAKT = {ATTRIBUTE_UNKNOWN, "TAKT_TAG_HVZ"};

	private final String[] ATTRIBUTE_L_NAME = {"NAME", "NAME"};
	private final String[] ATTRIBUTE_L_TCODE = {"TSYSCODE", "VSYSCODE"};
	private final String[] ATTRIBUTE_L_VEHCOMBNO = {"VEHCOMBNO", "FZGKOMBNR"};

	private final String[] ATTRIBUTE_LRI_LRNAME = {"LINEROUTENAME", "LINROUTENAME"};
	private final String[] ATTRIBUTE_LRI_LNAME = {"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_LRI_ID = {"INDEX", "INDEX"};
	private final String[] ATTRIBUTE_LRI_DCODE = {"DIRECTIONCODE", "RICHTUNGCODE"};
	private final String[] ATTRIBUTE_LRI_NODEID = {ATTRIBUTE_UNKNOWN, "KNOTNR"};
	private final String[] ATTRIBUTE_LRI_SPNO = {"STOPPOINTNO", "HPUNKTNR"};

	private final String[] ATTRIBUTE_TP_LNAME ={"LINENAME", "LINNAME"};
	private final String[] ATTRIBUTE_TP_LRNAME = {"LINEROUTENAME", "LINROUTENAME"};
	private final String[] ATTRIBUTE_TP_ID ={"NAME", "NAME"};
	private final String[] ATTRIBUTE_TP_DCODE ={"DIRECTIONCODE", "RICHTUNGCODE"};
	private final String[] ATTRIBUTE_TP_VEHCOMBNO = {"VEHCOMBNO", "FZGKOMBNR"};

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

	private final String[] ATTRIBUTE_VEHUNIT_ID = {"NO", "NR"};
	private final String[] ATTRIBUTE_VEHUNIT_CODE = {"CODE", "CODE"};
	private final String[] ATTRIBUTE_VEHUNIT_SEATCAP = {"SEATCAP", "SITZPL"};
	private final String[] ATTRIBUTE_VEHUNIT_TOTALCAP = {"TOTALCAP", "GESAMTPL"};

	private final String[] ATTRIBUTE_VEHCOMB_NO = {"NO", "NR"};
	private final String[] ATTRIBUTE_VEHCOMB_NAME = {"NAME", "NAME"};

	private final String[] ATTRIBUTE_VEHUNITTOVEHCOMB_VEHCOMBNO = {"VEHCOMBNO", "FZGKOMBNR"};
	private final String[] ATTRIBUTE_VEHUNITTOVEHCOMB_VEHUNITNO = {"VEHUNITNO", "FZGEINHEITNR"};
	private final String[] ATTRIBUTE_VEHUNITTOVEHCOMB_NUMVEHUNITS = {"NUMVEHUNITS", "ANZFZGEINH"};

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
				} else if (line.startsWith(this.TABLE_STOP[this.language])) {
					readStops(line, reader);
				} else if (line.startsWith(this.TABLE_NODE[this.language])) {
					readNodes(line, reader);
				} else if (line.startsWith(this.TABLE_EDGE[this.language])) {
					readEdges(line, reader);
				} else if (line.startsWith(this.TABLE_EDGETYPE[this.language])) {
					readEdgeTypes(line, reader);
				} else if (line.startsWith(this.TABLE_STOPAREA[this.language])) {
					readStopAreas(line, reader);
				} else if (line.startsWith(this.TABLE_STOPPOINT[this.language])) {
					readStopPoints(line, reader);
				} else if (line.startsWith(this.TABLE_LINE[this.language])) {
					readLines(line, reader);
				} else if (line.startsWith(this.TABLE_LINEROUTE[this.language])) {
					readLineRoutes(line, reader);
				} else if (line.startsWith(this.TABLE_LINEROUTEITEM[this.language])) {
					readLineRouteItems(line, reader);
				} else if (line.startsWith(this.TABLE_TIMEPROFILE[this.language])) {
					readTimeProfile(line, reader);
				} else if (line.startsWith(this.TABLE_TIMEPROFILEITEM[this.language])) {
					readTimeProfileItems(line, reader);
				} else if (line.startsWith(this.TABLE_VEHJOURNEY[this.language])) {
					readDepartures(line, reader);
				} else if (line.startsWith(this.TABLE_VEHUNIT[this.language])) {
					readVehicleUnits(line, reader);
				} else if (line.startsWith(this.TABLE_VEHCOMB[this.language])) {
					readVehicleCombinations(line, reader);
				} else if (line.startsWith(this.TABLE_VEHUNITTOVEHCOMB[this.language])) {
					readVehicleUnitToVehicleCombination(line, reader);
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
		if (line == null) {
			throw new RuntimeException("Language definition cannot be found.");
		}
		final String[] parts = StringUtils.explode(line, ';');
		if (parts[idxLanguage].equals("ENG")) {
			this.language = 0;
		} else if (parts[idxLanguage].equals("DEU")) {
			this.language = 1;
		} else {
			throw new RuntimeException("Unknown language: " + parts[idxLanguage]);
		}
		// proceed to next line, assumed to be empty
		reader.readLine();
	}

	private void readNodes(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_NODE[this.language].length()), ';');
		final int idxNo = getAttributeIndex(this.ATTRIBUTE_NODE_NO[this.language], attributes);
		final int idxName = getAttributeIndex(this.ATTRIBUTE_NODE_NAME[this.language], attributes);
		final int idxXcoord = getAttributeIndex(this.ATTRIBUTE_NODE_XCOORD[this.language], attributes);
		final int idxYcoord = getAttributeIndex(this.ATTRIBUTE_NODE_YCOORD[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.Node node = new VisumNetwork.Node(new IdImpl(parts[idxNo]), parts[idxName],
					new CoordImpl(Double.parseDouble(parts[idxXcoord].replace(',', '.')), Double.parseDouble(parts[idxYcoord].replace(',', '.'))));
			this.network.addNode(node);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readEdges(String tableAttributes, BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_EDGE[this.language].length()), ';');
		final int idxNo = getAttributeIndex(this.ATTRIBUTE_EDGE_NO[this.language], attributes);
		final int idxFromNode = getAttributeIndex(this.ATTRIBUTE_EDGE_FROM_NODE[this.language], attributes);
		final int idxToNode = getAttributeIndex(this.ATTRIBUTE_EDGE_TO_NODE[this.language], attributes);
		final int idxLength = getAttributeIndex(this.ATTRIBUTE_EDGE_LENGTH[this.language], attributes);
		final int idxEdgeTypeId = getAttributeIndex(this.ATTRIBUTE_EDGE_EDGETYPEID[this.language], attributes);
		
		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			IdImpl id = new IdImpl(parts[idxNo]);
			IdImpl fromNodeId = new IdImpl(parts[idxFromNode]);
			IdImpl toNodeId = new IdImpl(parts[idxToNode]);
			VisumNetwork.Edge lastEdge = this.network.edges.get(id);
			if (lastEdge != null) {
				if (lastEdge.fromNode.equals(toNodeId) && lastEdge.toNode.equals(fromNodeId)) {
					id = new IdImpl(parts[idxNo] + 'R');
				} else {
					throw new RuntimeException("Duplicate edge.");
				}
			}
			double length = Double.parseDouble(parts[idxLength].replace(',', '.'));
			VisumNetwork.Edge edge = new VisumNetwork.Edge(id,
					fromNodeId, toNodeId, length);
			String edgeTypeIdString = parts[idxEdgeTypeId];
			if (!edgeTypeIdString.isEmpty()) {
				IdImpl edgeTypeId = new IdImpl(edgeTypeIdString);
				edge.edgeTypeId = edgeTypeId;
			}
			this.network.addEdge(edge);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readEdgeTypes(String tableAttributes, BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_EDGETYPE[this.language].length()), ';');
		final int idxNo = getAttributeIndex(this.ATTRIBUTE_EDGETYPE_NO[this.language], attributes);
		final int idxKapIV = getAttributeIndex(this.ATTRIBUTE_EDGETYPE_KAPIV[this.language], attributes);
		final int idxV0IV = getAttributeIndex(this.ATTRIBUTE_EDGETYPE_V0IV[this.language], attributes);
		
		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.EdgeType edgeType = new VisumNetwork.EdgeType(new IdImpl(parts[idxNo]), parts[idxKapIV], parts[idxV0IV]);
			this.network.addEdgeType(edgeType);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readStops(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_STOP[this.language].length()), ';');
		final int idxNo = getAttributeIndex(this.ATTRIBUTE_STOP_NO[this.language], attributes);
		final int idxName = getAttributeIndex(this.ATTRIBUTE_STOP_NAME[this.language], attributes);
		final int idxXcoord = getAttributeIndex(this.ATTRIBUTE_STOP_XCOORD[this.language], attributes);
		final int idxYcoord = getAttributeIndex(this.ATTRIBUTE_STOP_YCOORD[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.Stop stop = new VisumNetwork.Stop(new IdImpl(parts[idxNo]), parts[idxName],
					new CoordImpl(Double.parseDouble(parts[idxXcoord].replace(',', '.')), Double.parseDouble(parts[idxYcoord].replace(',', '.'))));
			this.network.addStop(stop);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readStopAreas(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_STOPAREA[this.language].length()), ';');
		final int idxNo = getAttributeIndex(this.ATTRIBUTE_STOPAREA_NO[this.language], attributes);
		final int idxStopId = getAttributeIndex(this.ATTRIBUTE_STOPAREA_STOPNO[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.StopArea stopAr = new VisumNetwork.StopArea(new IdImpl(parts[idxNo]), new IdImpl(parts[idxStopId]));
			this.network.addStopArea(stopAr);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readStopPoints(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring( this.TABLE_STOPPOINT[this.language].length()), ';');
		final int idxNo = getAttributeIndex(this.ATTRIBUTE_STOPPT_NO[this.language], attributes);
		final int idxStopAreaNo = getAttributeIndex(this.ATTRIBUTE_STOPPT_STOPAREANO[this.language], attributes);
		final int idxName = getAttributeIndex(this.ATTRIBUTE_STOPPT_NAME[this.language], attributes);
		final int idxRLNo = getAttributeIndex(this.ATTRIBUTE_STOPPT_RLNO[this.language], attributes);
		final int idxNode = getAttributeIndex(this.ATTRIBUTE_STOPPT_NODE[this.language], attributes);
		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.StopPoint stopPt = new VisumNetwork.StopPoint(new IdImpl(parts[idxNo]), new IdImpl(parts[idxStopAreaNo]),parts[idxName], new IdImpl(parts[idxRLNo]), new IdImpl(parts[idxNode]));
			this.network.addStopPoint(stopPt);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLineRoutes(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_LINEROUTE[this.language].length()), ';');
		final int idxName = getAttributeIndex(this.ATTRIBUTE_LR_NAME[this.language], attributes);
		final int idxLineName = getAttributeIndex(this.ATTRIBUTE_LR_LINENAME[this.language], attributes);
		final int idxDCode = getAttributeIndex(this.ATTRIBUTE_LR_DCODE[this.language], attributes);
		final int idxTakt = getAttributeIndex(this.ATTRIBUTE_LR_TAKT[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.TransitLineRoute lr1 = new VisumNetwork.TransitLineRoute(new IdImpl(parts[idxName]), new IdImpl(parts[idxLineName]),new IdImpl(parts[idxDCode]));
			if (idxTakt != -1) {
				lr1.takt = parts[idxTakt];
			}
			this.network.addLineRoute(lr1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLines(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_LINE[this.language].length()), ';');
		final int idxName = getAttributeIndex(this.ATTRIBUTE_L_NAME[this.language], attributes);
		final int idxTCode = getAttributeIndex(this.ATTRIBUTE_L_TCODE[this.language], attributes);
		final int idxVehCombNo = getAttributeIndex(this.ATTRIBUTE_L_VEHCOMBNO[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			VisumNetwork.TransitLine tLine = new VisumNetwork.TransitLine(new IdImpl(parts[idxName]),parts[idxTCode], parts[idxVehCombNo]);
			this.network.addline(tLine);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readLineRouteItems(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_LINEROUTEITEM[this.language].length()), ';');
		final int idxLineRouteName = getAttributeIndex(this.ATTRIBUTE_LRI_LRNAME[this.language], attributes);
		final int idxLineName = getAttributeIndex(this.ATTRIBUTE_LRI_LNAME[this.language], attributes);
		final int idxIndex = getAttributeIndex(this.ATTRIBUTE_LRI_ID[this.language], attributes);
		final int idxDCode = getAttributeIndex(this.ATTRIBUTE_LRI_DCODE[this.language], attributes);
		final int idxNodeId = getAttributeIndex(this.ATTRIBUTE_LRI_NODEID[this.language], attributes);
		final int idxStopPointNo = getAttributeIndex(this.ATTRIBUTE_LRI_SPNO[this.language], attributes);


		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');
			IdImpl nodeId = new IdImpl(parts[idxNodeId]);
			String stopPointNoString = parts[idxStopPointNo];
			IdImpl stopPointNo;
			if (stopPointNoString.length() == 0) {
				stopPointNo = null;
			} else {
				stopPointNo = new IdImpl(stopPointNoString);
			}
			VisumNetwork.LineRouteItem lri1 = new VisumNetwork.LineRouteItem(parts[idxLineName],parts[idxLineRouteName],parts[idxIndex],parts[idxDCode],nodeId,stopPointNo);
			this.network.addLineRouteItem(lri1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readTimeProfile(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_TIMEPROFILE[this.language].length()), ';');
		final int idxLineName = getAttributeIndex(this.ATTRIBUTE_TP_LNAME[this.language], attributes);
		final int idxLineRouteName = getAttributeIndex(this.ATTRIBUTE_TP_LRNAME[this.language], attributes);
		final int idxIndex = getAttributeIndex(this.ATTRIBUTE_TP_ID[this.language], attributes);
		final int idxDCode = getAttributeIndex(this.ATTRIBUTE_TP_DCODE[this.language], attributes);
		final int idxVehCombNo = getAttributeIndex(this.ATTRIBUTE_TP_VEHCOMBNO[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');

			VisumNetwork.TimeProfile tp1 = new VisumNetwork.TimeProfile(new IdImpl(parts[idxLineName]),
					new IdImpl(parts[idxLineRouteName]), new IdImpl(parts[idxIndex]), new IdImpl(parts[idxDCode]),
					parts[idxVehCombNo]);
			this.network.addTimeProfile(tp1);
			// proceed to next line
			line = reader.readLine();
		}
	}
	private void readTimeProfileItems(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_TIMEPROFILEITEM[this.language].length()), ';');
		final int idxLineRouteName = getAttributeIndex(this.ATTRIBUTE_TPI_LRNAME[this.language], attributes);
		final int idxLineName = getAttributeIndex(this.ATTRIBUTE_TPI_LNAME[this.language], attributes);
		final int idxTPName = getAttributeIndex(this.ATTRIBUTE_TPI_TPNAME[this.language], attributes);
		final int idxDCode = getAttributeIndex(this.ATTRIBUTE_TPI_DCODE[this.language], attributes);
		final int idxIndex = getAttributeIndex(this.ATTRIBUTE_TPI_ID[this.language], attributes);
		final int idxArr = getAttributeIndex(this.ATTRIBUTE_TPI_ARR[this.language], attributes);
		final int idxDep = getAttributeIndex(this.ATTRIBUTE_TPI_DEP[this.language], attributes);
		final int idxLRIIndex = getAttributeIndex(this.ATTRIBUTE_TPI_LRIINDEX[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');

			VisumNetwork.TimeProfileItem tpi1 = new VisumNetwork.TimeProfileItem(parts[idxLineName],parts[idxLineRouteName],parts[idxTPName],parts[idxDCode],parts[idxIndex],parts[idxArr],parts[idxDep],new IdImpl(parts[idxLRIIndex]));
			this.network.addTimeProfileItem(tpi1);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readDepartures(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_VEHJOURNEY[this.language].length()), ';');
		final int idxLineRouteName = getAttributeIndex(this.ATTRIBUTE_D_LRNAME[this.language], attributes);
		final int idxLineName = getAttributeIndex(this.ATTRIBUTE_D_LNAME[this.language], attributes);
		final int idxIndex = getAttributeIndex(this.ATTRIBUTE_D_ID[this.language], attributes);
		final int idxTRI = getAttributeIndex(this.ATTRIBUTE_D_TPNAME[this.language], attributes);
		final int idxDep = getAttributeIndex(this.ATTRIBUTE_D_DEP[this.language], attributes);
		final int idxDCode = getAttributeIndex(this.ATTRIBUTE_D_DCODE[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');

			VisumNetwork.Departure d = new VisumNetwork.Departure(parts[idxLineName],parts[idxLineRouteName],parts[idxIndex],parts[idxTRI],parts[idxDep],new IdImpl(parts[idxDCode]));
			this.network.addDeparture(d);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readVehicleUnits(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_VEHUNIT[this.language].length()), ';');
		final int idxId = getAttributeIndex(this.ATTRIBUTE_VEHUNIT_ID[this.language], attributes);
		final int idxCode = getAttributeIndex(this.ATTRIBUTE_VEHUNIT_CODE[this.language], attributes);
		final int idxSeatCap = getAttributeIndex(this.ATTRIBUTE_VEHUNIT_SEATCAP[this.language], attributes);
		final int idxTotalCap = getAttributeIndex(this.ATTRIBUTE_VEHUNIT_TOTALCAP[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');

			VisumNetwork.VehicleUnit vehicleUnit = new VisumNetwork.VehicleUnit(parts[idxId], parts[idxCode],
					Integer.parseInt(parts[idxSeatCap]), Integer.parseInt(parts[idxTotalCap]));
			this.network.addVehicleUnit(vehicleUnit);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readVehicleCombinations(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_VEHCOMB[this.language].length()), ';');
		final int idxId = getAttributeIndex(this.ATTRIBUTE_VEHCOMB_NO[this.language], attributes);
		final int idxName = getAttributeIndex(this.ATTRIBUTE_VEHCOMB_NAME[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');

			VisumNetwork.VehicleCombination vehicleComb = new VisumNetwork.VehicleCombination(parts[idxId], parts[idxName]);
			this.network.addVehicleCombination(vehicleComb);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readVehicleUnitToVehicleCombination(final String tableAttributes, final BufferedReader reader) throws IOException {
		final String[] attributes = StringUtils.explode(tableAttributes.substring(this.TABLE_VEHUNITTOVEHCOMB[this.language].length()), ';');
		final int idxVehicleUnit = getAttributeIndex(this.ATTRIBUTE_VEHUNITTOVEHCOMB_VEHUNITNO[this.language], attributes);
		final int idxVehicleComb = getAttributeIndex(this.ATTRIBUTE_VEHUNITTOVEHCOMB_VEHCOMBNO[this.language], attributes);
		final int idxVehicleNumber = getAttributeIndex(this.ATTRIBUTE_VEHUNITTOVEHCOMB_NUMVEHUNITS[this.language], attributes);

		String line = reader.readLine();
		while (line != null && line.length() > 0) {
			final String[] parts = StringUtils.explode(line, ';');

			VisumNetwork.VehicleCombination vehicleComb = this.network.vehicleCombinations.get(parts[idxVehicleComb]);
			vehicleComb.vehUnitId = parts[idxVehicleUnit];
			vehicleComb.numOfVehicles = Integer.parseInt(parts[idxVehicleNumber]);
			// proceed to next line
			line = reader.readLine();
		}
	}

	private void readUnknownTable(final String tableAttributes, final BufferedReader reader) throws IOException {
		String line = reader.readLine();
		while (line != null && line.length() > 0) {

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
