/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package tutorial.converter.completeEventFilesRegardingVehicleInformation;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author tthunig
 *
 */
public class RunEventsConverterXML {

	// select the event file you want to convert
	private static String inputFile = "inputFile.xml";
	private static String outputFile = "outputFile.xml";
	
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
