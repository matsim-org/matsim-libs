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
import org.matsim.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.utils.io.tabularFileParser.TabularFileParserConfig;

import playground.marcel.visum.VisumNetwork;
import playground.marcel.visum.VisumNetworkReader;

public class VisumRuns {

	public static void findNearestStopExample() {
		// read visum network
		final VisumNetwork vNetwork = new VisumNetwork();
		try {
			new VisumNetworkReader(vNetwork).read("C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code/ptzh_orig.net");
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
			new VisumNetworkReader(vNetwork).read("C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code/ptzh_orig.net");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		/* read text file with segments
		 * expected format:
		 * 0:PersonID  1:TripID  2:SegmentID
		 * 3:XStartingPoint  4:YStartingPoint  5:ZStartingPoint  6:StartingDate  7:StartingTime
		 * 8:XEndingPoint  9:YEndingPoint  10:ZEndingPoint  11:EndingDate  12:EndingTime
		 * 13:Distance  14:TravelTime
		 * 15:Probability_Walk  16:Probability_Bike  17:Probability_Car  18:Probability_UrbanPuT  19:Probability_Rail
		 */
		final TabularFileParser parser = new TabularFileParser();
		final TabularFileParserConfig parserConfig = new TabularFileParserConfig();
		parserConfig.setFileName("C:\\Users\\yalcin\\Desktop\\Zurich\\Zurichdata\\Nadie\\Nadie_10.04.2008\\New FolderWalkAndPuTSegmentsYalcin_WithTime\\WalkAndPuTSegmentsYalcin_ZH/WalkAndPuTSegmentsYalcin_ZHl.txt");
		parserConfig.setDelimiterTags(new String[] { "\t" });
		SegmentsTableHandler handler = new SegmentsTableHandler(vNetwork, 0.6, "C:\\Users\\yalcin\\Desktop\\Zurich\\Marcel_code/results10.txt");

		try {
			// this will read the file AND write out the looked up data
			parser.parse(parserConfig, handler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		handler.finish();
	}


	public static void main(final String args[]) {
//		findNearestStopExample();
		findNearestStops();
		
	}

}
