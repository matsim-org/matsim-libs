/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.crossings.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import playground.polettif.crossings.analysis.LinkAnalysis;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class RunLinkTravelTimeAnalysis {
	
	public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {

		List<Id<Link>> crossingIds = new ArrayList<>();

		String base = "C:/Users/polettif/Desktop/crossings/";

		// set small
		String eventsFile = base + "output/small/ITERS/it.0/0.events.xml.gz";
		String networkFile = base + "output/small/output_network.xml.gz";
		String outputCSVtt = base + "analysis/small_travelTimes_" + System.nanoTime() + ".csv";
		String outputCSVvol = base + "analysis/small_linkVolumes_" + System.nanoTime() + ".csv";
		String outputCSVvolXY = base + "analysis/small_linkVolumesXY_" + System.nanoTime() + ".csv";
		String outputCSVtx = base + "analysis/small_timeSpace_" + System.nanoTime() + ".csv";
		crossingIds.add(Id.createLinkId("23"));
		crossingIds.add(Id.createLinkId("34"));
		crossingIds.add(Id.createLinkId("45"));

		LinkAnalysis linkAnalysis = new LinkAnalysis(crossingIds, eventsFile, "05:50:00", "06:15:00", networkFile);
//		linkAnalysis.runTravelTimeAnalysis(outputCSVtt);
//		linkAnalysis.runLinkVolumeAnalysis(outputCSVvol);
		linkAnalysis.runLinkVolumeAnalysisXY(outputCSVvolXY);
//		linkAnalysis.runTimeSpaceAnalysis(outputCSVtx);

		// set for pt-tutorial
//		linkIdsPtTutorial.put(Id.createLinkId("1222-x22"), Id.createLinkId("1222"));
//		linkIdsPtTutorial.put(Id.createLinkId("2212-x12"), Id.createLinkId("2212"));
//		linkIdsPtTutorial.put(Id.createLinkId("2322-x22"), Id.createLinkId("2322"));
//		linkIdsPtTutorial.put(Id.createLinkId("2223-x23"), Id.createLinkId("2223"));
//		linkIdsPtTutorial.put(Id.createLinkId("2232-x32"), Id.createLinkId("2232"));
//		linkIdsPtTutorial.put(Id.createLinkId("3222-x22"), Id.createLinkId("3222"));
//		linkIdsPtTutorial.put(Id.createLinkId("3231-x31"), Id.createLinkId("3231"));
//		linkIdsPtTutorial.put(Id.createLinkId("3132-x32"), Id.createLinkId("3132"));

//		crossingIds.add(Id.createLinkId("1222-x"));
//		crossingIds.add(Id.createLinkId("2212-x"));
//		crossingIds.add(Id.createLinkId("2322-x"));
//		crossingIds.add(Id.createLinkId("2223-x"));

	}

}