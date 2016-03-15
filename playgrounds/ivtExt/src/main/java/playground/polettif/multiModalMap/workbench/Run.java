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


package playground.polettif.multiModalMap.workbench;

import GTFS2PTSchedule.GTFS2MATSimTransitSchedule;

public class Run {

	public static void main(String[] args) throws Exception {
		String base = "C:/Users/polettif/Desktop/";
		String osm = "input/osm/zh_plus.osm";
		String osmNetwork1 = "output/osm/network_LV03plus.xml";
		String osmNetwork2 = "output/osm/network_WGS84.xml";

		String gtfs = "output/gtfs2MATSimTransitSchedule/transitSchedule.xml";
		String outputNetwork = "output/osm/network_gtfs.xml";

		// generate network from osm
//		String[] inputOSM = {base+osm, base+osmNetwork1};
//		Osm2Network.main(inputOSM);

		// Transform network "CH1903_LV03_Plus", "WGS84"
//		TransformNetworkFile.run(base+osmNetwork1, base+osmNetwork2, "CH1903_LV03_Plus", "WGS84");

		String[] inputGTFS = {base + gtfs, base + osmNetwork2, base + outputNetwork, "Test"};
		GTFS2MATSimTransitSchedule.main(inputGTFS);

	}

}
