/* *********************************************************************** *
 * project: matsim
 * EventsToXYData.java
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
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.io.IOUtils;

/*
 * Creates xy-data from an events file for a given transport mode.
 */
public class EventsToXYData implements BasicEventHandler {

	private static final String newLine = "\n";
	private static final String separator = "\t";

	private final Network network;
	private final Set<String> observedModes;	
	private final Map<Id, String> observedAgents = new HashMap<Id, String>();	// personId, transportMode
	private final Map<Id, LinkEnterEvent> linkEnterEvents = new HashMap<Id, LinkEnterEvent>();
	
	private BufferedWriter bufferedWriter;
	
	public static void main(String[] args) {
//		String inputEventsFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
//		String inputNetworkFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/input/network.xml.gz";
//		String outputXYFile = "/home/cdobler/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_walk.xy.gz";

		String inputEventsFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events.xml.gz";
		String inputNetworkFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/input/network.xml";
		String outputXYFile = "D:/Users/Christoph/workspace/matsim/mysimulations/ped2012/output/ITERS/it.0/0.events_walk.xy.gz";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputNetworkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new EventsToXYData(scenario.getNetwork(), "walk").createXYData(inputEventsFile, outputXYFile);
	}
	
	public EventsToXYData(Network network, String observedModes) {
		this(network, CollectionUtils.stringToSet(observedModes));
	}
	
	public EventsToXYData(Network network, Set<String> observedModes) {
		this.network = network;
		this.observedModes = observedModes;
	}
	
	public void createXYData(String inputEventsFile, String outputXYFile) {
		
		try {
			bufferedWriter = IOUtils.getAppendingBufferedWriter(outputXYFile);
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
	public void handleEvent(Event event) {
		
		// check whether the agent's mode is walk2d
		if (event instanceof PersonDepartureEvent) {
			PersonDepartureEvent agentDepartureEvent = (PersonDepartureEvent) event;
			if (observedModes.contains(agentDepartureEvent.getLegMode())) {
				observedAgents.put(agentDepartureEvent.getPersonId(), agentDepartureEvent.getLegMode());
				
				// add dummy position outside the visibla area
				writeLine(event.getTime() - Double.MIN_VALUE, agentDepartureEvent.getPersonId(), Double.MAX_VALUE,
						Double.MAX_VALUE, 0.0, 0.0, 0.0, agentDepartureEvent.getLegMode());
			}
			else return;
		}
		
		// store link enter events of observed agents
		else if (event instanceof LinkEnterEvent) {
			LinkEnterEvent linkEnterEvent = (LinkEnterEvent) event;
			if (observedAgents.containsKey(linkEnterEvent.getDriverId())) {
				linkEnterEvents.put(linkEnterEvent.getDriverId(), linkEnterEvent);
			}
		}
		
		// create xy data for link trips of observed agents
		else if (event instanceof LinkLeaveEvent) {
			LinkLeaveEvent linkLeaveEvent = (LinkLeaveEvent) event;
			Id personId = linkLeaveEvent.getDriverId();
			LinkEnterEvent linkEnterEvent = linkEnterEvents.remove(personId);
			
			if (linkEnterEvent != null) {
				Link link = this.network.getLinks().get(linkEnterEvent.getLinkId());
				double length = link.getLength();
				Coord fromCoord = link.getFromNode().getCoord();
				Coord toCoord = link.getToNode().getCoord();
				double fromTime = linkEnterEvent.getTime();
				double toTime = linkLeaveEvent.getTime();
				if (fromTime == toTime) return;
				String mode = this.observedAgents.get(personId);
				
				createXYData(fromCoord, toCoord, length, fromTime, toTime, personId, mode);
			}
		}
		
		// create xy data for link trips of observed agents
		else if (event instanceof PersonArrivalEvent) {
			PersonArrivalEvent agentArrivalEvent = (PersonArrivalEvent) event;
			Id personId = agentArrivalEvent.getPersonId();
			LinkEnterEvent linkEnterEvent = linkEnterEvents.remove(personId);
			
			if (linkEnterEvent != null) {
				Link link = this.network.getLinks().get(linkEnterEvent.getLinkId());
				double length = link.getLength();
				Coord fromCoord = link.getFromNode().getCoord();
				Coord toCoord = link.getToNode().getCoord();
				double fromTime = linkEnterEvent.getTime();
				double toTime = agentArrivalEvent.getTime();
				if (fromTime == toTime) return;
				String mode = this.observedAgents.get(personId);
				
				createXYData(fromCoord, toCoord, length, fromTime, toTime, personId, mode);
				
				// add dummy position outside the visibla area
				writeLine(event.getTime() + Double.MIN_VALUE, agentArrivalEvent.getPersonId(), Double.MAX_VALUE,
						Double.MAX_VALUE, 0.0, 0.0, 0.0, agentArrivalEvent.getLegMode());
			}
		}
	}
	
	private void writeHeader() {
		try {
			bufferedWriter.write("time");
			bufferedWriter.write(separator);
			bufferedWriter.write("personId");
			bufferedWriter.write(separator);
			bufferedWriter.write("x");
			bufferedWriter.write(separator);
			bufferedWriter.write("y");
			bufferedWriter.write(separator);
			bufferedWriter.write("vx");
			bufferedWriter.write(separator);
			bufferedWriter.write("vy");
			bufferedWriter.write(separator);
			bufferedWriter.write("vxy");
			bufferedWriter.write(separator);
			bufferedWriter.write("mode");	
			bufferedWriter.write(newLine);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void createXYData(Coord fromCoord, Coord toCoord, double length, double fromTime, double toTime, Id personId, String mode) {

		double dx = toCoord.getX() - fromCoord.getX();
		double dy = toCoord.getY() - fromCoord.getY();
		double dxy = Math.sqrt(dx*dx + dy*dy);
		double dt = toTime - fromTime;
		
		// the link might be longer than the euclidean distance
		double scaleFactor = dxy / length; 
		
		double vx = scaleFactor * dx / dt;
		double vy = scaleFactor * dy / dt;
		double vxy = Math.sqrt(vx*vx + vy*vy);
		
		writeLine(fromTime, personId, fromCoord.getX(), fromCoord.getY(), vx, vy, vxy, mode);
		writeLine(toTime, personId, toCoord.getX(), toCoord.getY(), vx, vy, vxy, mode);
	}
	
	private void writeLine(double time, Id personId, double x, double y, double vx, double vy, double vxy, String mode) {
		try {			
			bufferedWriter.write(String.valueOf(time));	// time
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(personId.toString()));	// id
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(x));	// x
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(y));	// y
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(vx));	// vx
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(vy));	// vy
			bufferedWriter.write(separator);
			bufferedWriter.write(String.valueOf(vxy));	// vxy
			bufferedWriter.write(separator);
			bufferedWriter.write(mode);	// mode					
			bufferedWriter.write(newLine);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset(int iteration) {
		observedAgents.clear();
		linkEnterEvents.clear();
	}
}
