/* *********************************************************************** *
 * project: org.matsim.*
 * Events2Stats.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.evacuation.events2stats;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.utils.io.IOUtils;

public class Events2Stats implements AgentDepartureEventHandler, AgentArrivalEventHandler, AgentMoneyEventHandler {

	HashMap<Id,Double> startTimes = new HashMap<Id,Double>();
	double oaTime = 0;
	double numAgents = 0;
	
	double numUtilityEvents = 0;
	double cumUt = 0;
	
	public void run(final String eventsFile, final BufferedWriter writer) {
		this.startTimes.clear();
		this.oaTime = 0;
		this.numAgents = 0;
		this.cumUt = 0;
		this.numUtilityEvents = 0;
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);
		
		new EventsReaderTXTv1(events).readFile(eventsFile);
		System.out.println(this.oaTime/this.numAgents + "   " + this.cumUt + "  " + this.cumUt/this.numUtilityEvents);
		try {
			writer.write(this.oaTime/this.numAgents + "," + this.cumUt + "," +  this.cumUt/this.numUtilityEvents+ "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void handleEvent(final AgentDepartureEvent event) {
		this.startTimes.put(event.getPersonId(), event.getTime());
		
	}

	public void reset(final int iteration) {
		
	}

	public void handleEvent(final AgentArrivalEvent event) {
		this.oaTime += event.getTime() - this.startTimes.get(event.getPersonId());
		this.numAgents++;
	}
	
	public void handleEvent(final AgentMoneyEvent event) {
		this.numUtilityEvents++;
		this.cumUt += event.getAmount();
		
	}
	
	public static void main(final String [] args) {
		String root = "../../outputs/output/ITERS";
		int start = 0;
		int stop = 200;
			
		BufferedWriter writer = null;
		try {
			 writer = IOUtils.getBufferedWriter("../../outputs/output/analysis/tt.csv", false);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			writer.write("it,avgTT,cumMSA,avgMSA\n");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		
		Events2Stats e2s = new Events2Stats();
		for (int i = start; i <= stop; i++) {
			try {
				writer.write(i + ",");
			} catch (IOException e) {
				e.printStackTrace();
			}
			String current = root + "/it." + i + "/" + i + ".events.txt.gz";
			System.out.println(current);
			e2s.run(current,writer);
			
		}
		
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
