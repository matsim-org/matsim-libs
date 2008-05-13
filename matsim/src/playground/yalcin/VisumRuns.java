/* *********************************************************************** *
 * project: org.matsim.*
 * VisumRuns.java
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

package playground.yalcin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

import org.matsim.utils.geometry.shared.Coord;
import org.matsim.utils.io.tabularFileParser.TabularFileHandlerI;
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.marcel.visum.VisumNetwork;
import playground.marcel.visum.VisumNetworkReader;

public class VisumRuns {


	public static void findNearestStopExample() {
		// read visum network
		final VisumNetwork vNetwork = new VisumNetwork();
		try {
			new VisumNetworkReader(vNetwork).read("../mystudies/yalcin/ptzh_orig.net");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		final Collection<VisumNetwork.Stop> stops = vNetwork.findStops(new Coord(683.5, 248.0), 0.8);
		System.out.println("found the following stops:");
		for (VisumNetwork.Stop stop : stops) {
			System.out.println("id=" + stop.id + " x=" + stop.coord.getX() + " y=" + stop.coord.getY() + " name=" + stop.name);
		}
	}


	public static void findNearestStops() {
		// read visum network
		final VisumNetwork vNetwork = new VisumNetwork();
		try {
			new VisumNetworkReader(vNetwork).read("../mystudies/yalcin/ptzh_orig.net");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		/* read text file with coordinates
		 * expected format:
		 * ID1  ID2  X1  Y1  X2  Y2
		 */
		final TabularFileParser parser = new TabularFileParser();
		final TabularFileParserConfig parserConfig = new TabularFileParserConfig();
		parserConfig.setDelimiterTags(new String[] { "\t" });
		try {
			parser.parse(parserConfig, new TabularFileHandlerI() {
				public void startRow(final String[] row) {
					final Coord coord1 = new Coord(Double.parseDouble(row[2]), Double.parseDouble(row[3]));
					final Coord coord2 = new Coord(Double.parseDouble(row[4]), Double.parseDouble(row[5]));
					final Collection<VisumNetwork.Stop> stop1 = vNetwork.findStops(coord1, 0.8);
					final Collection<VisumNetwork.Stop> stop2 = vNetwork.findStops(coord2, 0.8);
				}
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static void main(final String args[]) {
		findNearestStopExample();
//		findNearestStops();
	}

}
