/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.munich.testroad;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

/**
 * @author benjamin
 *
 */
public class SimpleControlerListener implements StartupListener, ShutdownListener {

	private TravelTimeEventHandler eventHandler;
	private SortedMap<Id, Double> personId2travelTimes = new TreeMap<Id, Double>();
	private SortedMap<Id, Double> personId2enterTimes = new TreeMap<Id, Double>();

	private Scenario scenario;
	private Id enterLinkId;
	private Id leaveLinkId;
	private Integer leaveLinkCapacity;

	public SimpleControlerListener(Scenario scenario, Id enterLinkId, Id leaveLinkId, Integer leaveLinkCapacity) {
		this.scenario = scenario;
		this.enterLinkId = enterLinkId;
		this.leaveLinkId = leaveLinkId;
		this.leaveLinkCapacity = leaveLinkCapacity;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.eventHandler = new TravelTimeEventHandler(personId2travelTimes, personId2enterTimes, enterLinkId, leaveLinkId);
		event.getControler().getEvents().addHandler(this.eventHandler);
		
		Link link = scenario.getNetwork().getLinks().get(this.leaveLinkId);
		link.setCapacity(leaveLinkCapacity);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		SortedMap<Double, Double> enterTimes2travelTimes = createEnterTimes2travelTimes(this.personId2travelTimes, this.personId2enterTimes);
		
		System.out.println(this.personId2travelTimes);
		System.out.println(this.personId2enterTimes);
		System.out.println(enterTimes2travelTimes);
		
		writeTable(enterTimes2travelTimes, "enterTimes2travelTimes");
	}

	private SortedMap<Double, Double> createEnterTimes2travelTimes(SortedMap<Id, Double> personId2travelTimes, SortedMap<Id, Double> personId2enterTimes) {
		SortedMap<Double, Double> enterTimes2travelTimes = new TreeMap<Double, Double>();
		for(Id personId : personId2enterTimes.keySet()){
				Double enterTime = personId2enterTimes.get(personId);
				Double travelTime = personId2travelTimes.get(personId);
				
				enterTimes2travelTimes.put(enterTime, travelTime);
		}
		return enterTimes2travelTimes;
	}

	private void writeTable(SortedMap<Double, Double> enterTimes2travelTimes, String outputName) {
		String outputPath = this.scenario.getConfig().controler().getOutputDirectory() + "/";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath + outputName + ".txt")));

			for(Entry <Double, Double> entry : enterTimes2travelTimes.entrySet()){
				Integer enterTime = ((Double) entry.getKey()).intValue();
				Integer travelTime = ((Double) entry.getValue()).intValue();
				
				bw.write(enterTime.toString());
				bw.write(";");
				bw.write(travelTime.toString());
				bw.newLine();
			}
			bw.close();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
