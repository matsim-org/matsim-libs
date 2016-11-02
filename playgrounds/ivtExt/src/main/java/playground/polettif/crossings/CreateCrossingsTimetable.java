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

package playground.polettif.crossings;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;
import playground.polettif.crossings.lib.LinkChangeEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Creates a timetable (network change events file) with a given
 * network, crossings and events file.
 *
 * @author polettif
 */
public class CreateCrossingsTimetable {

	private static final Logger log = Logger.getLogger(CrossingsFileParser.class);

	/**
	 * Creates a timetable (network change events file)
	 * @param args [0] inputNetworkFile<br>
	 *             [1] inputCrossingsFile<br>
	 *             [2] inputEventsFile<br>
	 *             [3] outputNetworkChangeEventsFile<br>
	 *             [4] preBuffer<br>
	 *             [5] postBuffer<br>
	 */
	public static void main(String[] args) {
		double preBuffer = Double.parseDouble(args[4]);
		double postBuffer = Double.parseDouble(args[5]);

		run(args[0], args[1], args[2], args[3], preBuffer, postBuffer);
	}

	public static void run(String inputNetworkFile, String inputCrossingsFile, 
			String inputEventsFile, String outputNetworkChangeEventsFile, double preBuffer, double postBuffer) {
		
		// generate basic Config
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(inputNetworkFile);

		//create an event object
		EventsManager eventsManager = EventsUtils.createEventsManager();
        			
		//create the handler and add it
		CrossingEventHandler handler = new CrossingEventHandler();
		handler.setNetwork(network);
		handler.setBuffer(preBuffer, postBuffer);
		handler.loadCrossings(inputCrossingsFile);
		eventsManager.addHandler(handler);

		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(inputEventsFile);
		List<LinkChangeEvent> linkChangeEvents = handler.getLinkChangeEvents();

		// write networkChangeEvents file
		WriteNetworkChangeEvents(linkChangeEvents, outputNetworkChangeEventsFile);
	}

	/**
	 * Writes the network change events file with a given list of link change events
	 */
	private static void WriteNetworkChangeEvents(List<LinkChangeEvent> linkChangeEvents, String filepath) {
			
		final BufferedWriter out = IOUtils.getBufferedWriter(filepath);
			try {
				// Header
				out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				out.newLine();
				out.write("<networkChangeEvents xmlns=\"http://www.matsim.org/files/dtd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/networkChangeEvents.xsd\">");
				out.newLine(); out.newLine();
				
				// go through ChangeEvents
				for(LinkChangeEvent event : linkChangeEvents) {
					out.write("\t<networkChangeEvent startTime=\"" + event.getStarttime() + "\">");
					out.newLine();
					out.write("\t\t<link refId=\"" + event.getLinkId() + "\"/>");
					out.newLine();
					out.write("\t\t<link refId=\"" + event.getLinkId() + "\"/>");
					out.newLine();
					out.write("\t\t<flowCapacity type=\"absolute\" value=\"0\"/>");
					out.newLine();
					out.write("\t</networkChangeEvent>");
					out.newLine();
					out.write("\t<networkChangeEvent startTime=\"" + event.getStoptime() + "\">");
					out.newLine();
					out.write("\t\t<link refId=\"" + event.getLinkId() + "\"/>");
					out.newLine();
					out.write("\t\t<link refId=\"" + event.getLinkId() + "\"/>");
					out.newLine();
					out.write("\t\t<flowCapacity type=\"absolute\" value=\"" + event.getCapacity() + "\"/>");
					out.newLine();
					out.write("\t</networkChangeEvent>");
					out.newLine();
					out.newLine();
				}
				
				// Footer:
				out.write("</networkChangeEvents>");
				out.newLine();
				out.close();
				log.info("networkChangeEvents file written!");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

}
