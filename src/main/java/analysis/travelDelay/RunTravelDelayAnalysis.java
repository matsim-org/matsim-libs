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

package analysis.travelDelay;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import analysis.drtOccupancy.DynModeTripsAnalyser;

public class RunTravelDelayAnalysis {

	public static void main(String[] args) {

		String runDir = "D:\\be_251\\";
		String runId = "be_251.";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(runDir + runId + "output_network.xml.gz");
		TravelDelayCalculator tdc = new TravelDelayCalculator(network);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(tdc);
		new MatsimEventsReader(events).readFile(runDir + runId + "output_events.xml.gz");
		DynModeTripsAnalyser.collection2Text(tdc.getTrips(), runDir+"delays.csv", "PersonId;ArrivalTime;FreespeedTravelTime;ActualTravelTime");
	}
}
