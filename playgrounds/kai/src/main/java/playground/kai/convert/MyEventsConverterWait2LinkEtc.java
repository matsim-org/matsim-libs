/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package playground.kai.convert;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

import tutorial.converter.completeEventFilesRegardingVehicleInformation.EventsConverterXML;

/**
 * @author nagel
 *
 */
public class MyEventsConverterWait2LinkEtc {
	// select the event file you want to convert
	private static String inputFile = "/Users/nagel/git/matsim/playgrounds/wrashid/test/input/playground/wrashid/PSF2/pluggable/agent2UsesCarNotAsModeForFirstLeg.events.xml";
	private static String outputFile = "/Users/nagel/kw/events.xml";

	public static void main(String[] args) {

		if (args != null && args.length !=  0){
			inputFile = args[0];
		}

		EventsManager em = EventsUtils.createEventsManager();
		EventWriterXML eventWriter = new EventWriterXML(outputFile);
		em.addHandler(eventWriter);

		EventsConverterXML converter = new EventsConverterXML(em);
		converter.readFile(inputFile);

		eventWriter.closeFile();
	}


}
