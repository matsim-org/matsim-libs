/* *********************************************************************** *
 * project: matsim
 * TransportModeFilter.java
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/*
 * Create an events file that contains only arrival, departure and link events of
 * agents using a given transport mode.
 */
public class ParkedVehiclesToXYData implements BasicEventHandler {
	
	private static final String newLine = "\n";
	private static final String separator = "\t";
	
	private final Map<Id, Id> observedAgents = new HashMap<Id, Id>();
	private final Network network;

	private BufferedWriter bufferedWriter;
	
	public static void main(String[] args) {
//		String inputEventsFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
//		String inputNetworkFile = /home/cdobler/workspace/matsim/mysimulations/ped2012/input/network.xml";
//		String outputXYFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_parked_vehicles.xy.gz";		
		
		String inputEventsFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String inputNetworkFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input/network.xml";
		String outputXYFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_parked_vehicles.xy.gz";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputNetworkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new ParkedVehiclesToXYData(scenario.getNetwork()).filterFile(inputEventsFile, outputXYFile);
	}
			
	public ParkedVehiclesToXYData(Network network) {
		this.network = network;
	}
	
	public void filterFile(String inputEventsFile, String outputXYFile) {		
		
		try {
			bufferedWriter = IOUtils.getBufferedWriter(outputXYFile);
			writeHeader();
			
			EventsManager eventsManager = EventsUtils.createEventsManager();
			MatsimEventsReader eventReader = new MatsimEventsReader(eventsManager);
			
			eventsManager.addHandler(this);
			eventReader.readFile(inputEventsFile);
			
			bufferedWriter.flush();
			bufferedWriter.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void reset(int iteration) {
		observedAgents.clear();
	}

	@Override
	public void handleEvent(Event event) {
		
		// remove the vehicle
		if (event instanceof PersonEntersVehicleEvent) {
			PersonEntersVehicleEvent personEntersVehicleEvent = (PersonEntersVehicleEvent) event;
			for (int i = 0; i < 3; i++) {
				String id = personEntersVehicleEvent.getVehicleId().toString() + "_" + i;
				createXYData(event.getTime(), id, Double.MAX_VALUE, Double.MAX_VALUE);
			}
		} 
		
		// add the vehicle
		else if (event instanceof PersonLeavesVehicleEvent) {
			PersonLeavesVehicleEvent personLeavesVehicleEvent = (PersonLeavesVehicleEvent) event;
			
			String vehicleId = personLeavesVehicleEvent.getVehicleId().toString();
			Id linkId = observedAgents.get(personLeavesVehicleEvent.getPersonId());
			Link link = network.getLinks().get(linkId);
			
			// Check whether it is a parking lot. If not, return.
			if (!linkId.toString().contains("parking")) return;
			
			Coord centerCoord = link.getCoord();
			Coord toCoord = link.getToNode().getCoord();
			Coord fromCoord = link.getFromNode().getCoord();
			
			double dx = toCoord.getX() - fromCoord.getX();
			double dy = toCoord.getY() - fromCoord.getY();
			double dxy = Math.sqrt(dx*dx + dy*dy);
			
			dx = dx / dxy;
			dy = dy / dxy;
			
			createXYData(event.getTime(), vehicleId + "_" + 0, centerCoord.getX(), centerCoord.getY());
			createXYData(event.getTime(), vehicleId + "_" + 1, centerCoord.getX() + dx, centerCoord.getY() + dy);
			createXYData(event.getTime(), vehicleId + "_" + 2, centerCoord.getX() - dx, centerCoord.getY() - dy);
		}
		
		// check whether the agent's mode is observed
		else if (event instanceof PersonDepartureEvent) {
			PersonDepartureEvent agentDepartureEvent = (PersonDepartureEvent) event;
			if (agentDepartureEvent.getLegMode().equals(TransportMode.car)) {
				observedAgents.put(agentDepartureEvent.getPersonId(), agentDepartureEvent.getLinkId());
			}
		}
		
		// get new agents' positions
		else if (event instanceof LinkEnterEvent) {
			LinkEnterEvent linkEnterEvent = (LinkEnterEvent) event;
			if (observedAgents.containsKey(linkEnterEvent.getDriverId())) {
				observedAgents.put(linkEnterEvent.getDriverId(), linkEnterEvent.getLinkId());
			}
		}
	}
	
	private void writeHeader() {
		try {
			bufferedWriter.write("time");
			bufferedWriter.write(separator);
			bufferedWriter.write("vehicleId");
			bufferedWriter.write(separator);
			bufferedWriter.write("x");
			bufferedWriter.write(separator);
			bufferedWriter.write("y");
			bufferedWriter.write(newLine);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void createXYData(double time, String vehicleId, double x, double y) {
		try {			
			bufferedWriter.write(String.valueOf(time));	// time
			bufferedWriter.write(separator);
			bufferedWriter.write(vehicleId);	// vehicleId
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(x));	// x
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(y));	// y
			bufferedWriter.write(newLine);			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}