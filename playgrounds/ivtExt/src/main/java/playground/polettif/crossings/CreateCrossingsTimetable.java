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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.polettif.crossings.parser.CrossingsParser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class CreateCrossingsTimetable {

	private static final Logger log = Logger.getLogger(CrossingsParser.class);

	public static void main(String[] args) {
		double preBuffer = Double.parseDouble(args[4]);
		double postBuffer = Double.parseDouble(args[5]);

		run(args[0], args[1], args[2], args[3], preBuffer, postBuffer);
	}

	public static void run(String inputNetworkFile, String inputCrossingsFile, 
			String inputEventsFile, String outputNetworkChangeEventsFile, double preBuffer, double postBuffer) {
		
		// generate basic Config
		Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", inputNetworkFile);
		
		// load scenario
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();
        			
		//create the handler and add it
		CrossingsHandler handler = new CrossingsHandler();
		handler.setNetwork(scenario.getNetwork());
		handler.setBuffer(preBuffer, postBuffer);
		handler.loadCrossings(inputCrossingsFile);
		events.addHandler(handler);

		//create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputEventsFile);
		
		List<LinkChangeEvent> linkChangeEvents = handler.getLinkChangeEvents();
		
		
		// write networkChangeEvents file
		WriteNetworkChangeEvents(linkChangeEvents, outputNetworkChangeEventsFile);
	}

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
					out.write("\t\t<link refId=\"" + event.getLinkId1() + "\"/>");
					out.newLine();
					out.write("\t\t<link refId=\"" + event.getLinkId2() + "\"/>");
					out.newLine();
					out.write("\t\t<flowCapacity type=\"absolute\" value=\"0\"/>");
					out.newLine();
					out.write("\t</networkChangeEvent>");
					out.newLine();
					out.write("\t<networkChangeEvent startTime=\"" + event.getStoptime() + "\">");
					out.newLine();
					out.write("\t\t<link refId=\"" + event.getLinkId1() + "\"/>");
					out.newLine();
					out.write("\t\t<link refId=\"" + event.getLinkId2() + "\"/>");
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
