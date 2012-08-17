/* *********************************************************************** *
 * project: org.matsim.*
 * SfAirScheduleBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package air.run;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author fuerbas
 * @author dgrether
 *
 */
public class SfFlightTimeControlerListener implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

	private SfFlightTimeEventHandler flightTimeHandler;
	private static final String SEPARATOR = "\t";


	
	@Override
	public void notifyStartup(StartupEvent event) {
		this.flightTimeHandler = new SfFlightTimeEventHandler();
		event.getControler().getEvents().addHandler(this.flightTimeHandler);		
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.writeStats(event);
		this.computeAndWriteDelay(event);
	}
	
	private void computeAndWriteDelay(IterationEndsEvent event) {
		
	}

	
	private void writeStats(IterationEndsEvent event){
		try {
			String filename = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "statistic.csv");
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			String header = "FlightNumber" + SEPARATOR + "ArrivalTime";
			writer.write(header);
			writer.newLine();
				
			for (Entry<Id, Double> entry : this.flightTimeHandler.returnArrival().entrySet()) {
						StringBuilder line = new StringBuilder();
						String idString = entry.getKey().toString();
						if (idString.contains("_")) {
							String[] keyEntries = idString.split("_");	//extracting flight number from personId
							line.append(keyEntries[1]);	//flight number
							line.append(SEPARATOR);
							line.append(entry.getValue());	//arrival time
							writer.append(line.toString());
							writer.newLine();
						}
					}						
			writer.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	

	@Override
	public void notifyShutdown(ShutdownEvent event) {
	}
	
	
//	DgCottbusSylviaAnalysisControlerListener
	

}
