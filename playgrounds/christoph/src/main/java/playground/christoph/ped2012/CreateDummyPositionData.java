/* *********************************************************************** *
 * project: org.matsim.*
 * CreateDummyPositionData.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.ped2012;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.IOUtils;

/*
 * So far, via moves xy data from point to point but cannot remove a point when
 * the agent which it represents starts an activity. Therefore we create dummy
 * xy data for those agents which contains xy data outside the visible area.
 */
public class CreateDummyPositionData implements BasicEventHandler {
	
	private static final String newLine = "\n";
	private static final String separator = "\t";
	
	private final Set<Id> observedAgents = new HashSet<Id>();
	private BufferedWriter bufferedWriter;
	
	public static void main(String[] args) {
//		String inputEventsFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
//		String inputXYFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xy.gz";
//		String outputXYFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_with_dummies.xy.gz";

		String inputEventsFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String inputXYFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xy.gz";
		String outputXYFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_with_dummies.xy.gz";
		
		new CreateDummyPositionData().createDummyData(inputEventsFile, inputXYFile, outputXYFile);
	}
	
	public void createDummyData(String inputEventsFile, String inputXYFile, String outputXYFile) {
		
		try {
			EventsManager eventsManager = EventsUtils.createEventsManager();
			MatsimEventsReader eventReader = new MatsimEventsReader(eventsManager);
			
			/*
			 * We cannot add text to an existing gzipped file. Therefore we open the existing file,
			 * write its content to a new file and then add the dummy data to that hew file.
			 */
			BufferedReader bufferedReader = IOUtils.getBufferedReader(inputXYFile);
			bufferedWriter = IOUtils.getBufferedWriter(outputXYFile);
			
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				bufferedWriter.write(line);
				bufferedWriter.write(newLine);
			}
			bufferedReader.close();
			
			eventsManager.addHandler(this);
			eventReader.readFile(inputEventsFile);

			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleEvent(Event event) {
		
		// check whether the agent's mode is walk2d
		if (event instanceof PersonDepartureEvent) {
			PersonDepartureEvent agentDepartureEvent = (PersonDepartureEvent) event;
			if (agentDepartureEvent.getLegMode().equals("walk2d")) observedAgents.add(agentDepartureEvent.getPersonId());
			else return;
		}
		
		// if an agent arrives check whether its mode was walk2d
		else if (event instanceof PersonArrivalEvent) {
			PersonArrivalEvent agentArrivalEvent = (PersonArrivalEvent) event;
			if (observedAgents.contains(agentArrivalEvent.getPersonId())) {
				observedAgents.remove(agentArrivalEvent.getPersonId());

				try {
					bufferedWriter.write(String.valueOf(event.getTime()));	// time
					bufferedWriter.write(separator);
					bufferedWriter.write(String.valueOf(agentArrivalEvent.getPersonId().toString()));	// id
					bufferedWriter.write(separator);
					bufferedWriter.write(String.valueOf(Double.MAX_VALUE));	// x
					bufferedWriter.write(separator);
					bufferedWriter.write(String.valueOf(Double.MAX_VALUE));	// y
					bufferedWriter.write(separator);
					bufferedWriter.write(String.valueOf(0.0));	// vx
					bufferedWriter.write(separator);
					bufferedWriter.write(String.valueOf(0.0));	// vy
					bufferedWriter.write(separator);
					bufferedWriter.write(String.valueOf(0.0));	// vxy
					bufferedWriter.write(newLine);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public void reset(int iteration) {
		observedAgents.clear();
	}
}