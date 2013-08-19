/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.andreas.virginia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;


/**
 * 
 * @author aneumann
 *
 */
public class Events2CSV {
	
	private static final Logger log = Logger.getLogger(Events2CSV.class);
	
	public static void main(String[] args) {
		
		Gbl.startMeasurement();
		
		if (args.length == 1) {
			// assume it's an event file - proceed
			if(new File(args[0]).exists()){
				Gbl.printElapsedTime(); Gbl.printMemoryUsage();
				Events2CSV.processEventsFile(args[0]);
				Gbl.printElapsedTime(); Gbl.printMemoryUsage();
			}else{
				log.error("Could not find the given file " + args[0]);
			}
		} else {
			log.error("Please provide an events file. Terminating.");
		}
		
	}

	private static void processEventsFile(String eventsFile) {
		String outputFileName = eventsFile.replace(".xml.gz", "");
		
		try {
			BufferedWriter agentWriter = IOUtils.getBufferedWriter(outputFileName + ".agentDeparturesAtFirstStop.csv");
			agentWriter.write("agentId, time");
			BufferedWriter vehicleWriter = IOUtils.getBufferedWriter(outputFileName + ".vehicleDepartsAtFirstStop.csv");
			vehicleWriter.write("vehicleId, time");
		
			Events2CSVHandler handler = new Events2CSVHandler();
			
			log.info("Start handling events...");
			EventsManager manager = EventsUtils.createEventsManager();
			manager.addHandler(handler);
			new MatsimEventsReader(manager).readFile(eventsFile);
			log.info("Finished reading events...");
			
			
			log.info("Creating output...");
			for (Entry<Id, Double> agentId2TimeEntry : handler.getAgentId2TimeMap().entrySet()) {
				agentWriter.newLine();
				agentWriter.write(agentId2TimeEntry.getKey() + ", " + agentId2TimeEntry.getValue());
			}
			
			for (Entry<Id, Double> vehicleId2TimeEntry : handler.getVehicle2TimeMap().entrySet()) {
				vehicleWriter.newLine();
				vehicleWriter.write(vehicleId2TimeEntry.getKey() + ", " + vehicleId2TimeEntry.getValue());
			}
			

			agentWriter.flush();
			agentWriter.close();

			vehicleWriter.flush();
			vehicleWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("All done");
	}

}
