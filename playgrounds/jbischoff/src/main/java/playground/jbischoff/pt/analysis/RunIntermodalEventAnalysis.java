/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.pt.analysis;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunIntermodalEventAnalysis {
public static void main(String[] args) {
	String networkFile = "C:/net/ils4/jbischoff/multimodal/output/r10-w1000_2_10_cl/r10-w1000_2_10_cl.output_network.xml.gz";
	String eventsFile = "C:/net/ils4/jbischoff/multimodal/output/r10-w1000_2_10_cl/r10-w1000_2_10_cl.output_events.xml.gz";
	String outFile = "C:/net/ils4/jbischoff/multimodal/output/r10-w1000_2_10_cl/intermodalChains.csv";
	
	Network network = NetworkUtils.createNetwork();
	new MatsimNetworkReader(network).readFile(networkFile);
	EventsManager events = EventsUtils.createEventsManager();
	IntermodalChainEventHandler intermodalChainEventHandler = new IntermodalChainEventHandler(network);
	events.addHandler(intermodalChainEventHandler);
	new MatsimEventsReader(events).readFile(eventsFile);
	intermodalChainEventHandler.writeToFile(outFile);
}
}
