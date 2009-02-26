/* *********************************************************************** *
 * project: org.matsim.*
 * VisumAnbindungstabelleWriter.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.world.Location;
import org.matsim.world.ZoneLayer;

/**
 * @author mrieser
 *
 * creates an "Anbindungstabelle" for use with VISUM.<br/>
 * The Anbindungstabelle contains information about which node is connected
 * to which zone (tvz, taz) for mapping the zone-based demand to the network
 * in VISUM.<br/>
 * The algorithm just searches for the nearest centroid of a zone for each
 * node and generates an Anbindung from the node to that zone.
 */
public class VisumAnbindungstabelleWriter {

	public VisumAnbindungstabelleWriter() {

	}

	public void write(String filename, NetworkLayer network, ZoneLayer zones) {
		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(filename));
			// HEADER
			out.write("$VISION\n");
			out.write("* Created by MATSim <www.matsim.org>\n");
			out.write("*\n");
			out.write("*\n");
			out.write("* Tabelle: Versionsblock\n");
			out.write("$VERSION:VERSNR;FILETYPE;LANGUAGE;UNIT\n");
			out.write("3.000;Net;DEU;KM\n");
			out.write("\n");
			out.write("*\n");
			out.write("*\n");
			out.write("* Tabelle: Anbindungen\n");
			out.write("$ANBINDUNG:BEZNR;KNOTNR;RICHTUNG;TYPNR;VSYSSET\n");

			for (Node node : network.getNodes().values()) {
				String visum = "";
				// ANBINDUNG:BEZNR
				ArrayList<Location> locs = zones.getNearestLocations(node.getCoord(), null);
				if (locs.size() > 0) {
					visum += locs.get(0).getId() + ";";
				} else {
					visum += "00;";
				}
				// KNOTNR
				visum += node.getId() + ";";
				// RICHTUNG;TYPNR;VSYSSET
				out.write(visum + "Q;1;P\n");
				out.write(visum + "Z;1;P\n");
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			if (out != null) {
				try { out.close(); } catch (IOException ignored) {}
			}
		}
	}

}
