/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package analysis.drtOccupancy;

import java.awt.dnd.DnDConstants;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;

public class RunDrtVehicleOccupancyDistanceAnalysis {

	public static void main(String[] args) {

		String runDir = "C:\\Users\\Joschka\\git\\matsim\\contribs\\drt\\output\\drt_door2door\\";
		String runId = "";
		int maxcap = 8;

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(runDir + runId + "output_network.xml.gz");
		EventsManager events = EventsUtils.createEventsManager();
		DynModePassengerStats dynModePassengerStats = new DynModePassengerStats(network, "drt", maxcap);
		events.addHandler(dynModePassengerStats);

		new MatsimEventsReader(events).readFile(runDir + "output_events.xml.gz");
		String vehOcc = DynModeTripsAnalyser
				.summarizeDetailedOccupancyStats(dynModePassengerStats.getVehicleDistances(), ";", maxcap);

		BufferedWriter bw2 = IOUtils.getAppendingBufferedWriter(runDir + runId + "drt_detailed_distanceStats.csv");
		try {

			bw2.write("runId;iteration");
			for (int i = 0; i <= maxcap; i++) {
				bw2.write(";" + i + " pax distance_m");
			}

			bw2.newLine();
			bw2.write(runId + ";" + 0 + vehOcc);
			bw2.flush();
			bw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
