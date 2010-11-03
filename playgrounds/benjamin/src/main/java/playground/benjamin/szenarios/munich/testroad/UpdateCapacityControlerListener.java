/* *********************************************************************** *
 * project: org.matsim.*
 * BkControlerListener.java
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
package playground.benjamin.szenarios.munich.testroad;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;


/**
 * @author benjamin
 *
 */
public class UpdateCapacityControlerListener implements StartupListener, IterationEndsListener, ShutdownListener {

	private TravelTimeEventHandler eventHandler;
	private SortedMap<Double, Double> departureTimes2travelTimes = new TreeMap<Double, Double>();
	private Scenario scenario;
	private Id linkid;

	/**
	 * @param scenario
	 */
	public UpdateCapacityControlerListener(Scenario scenario) {
		this.scenario = scenario;
		this.linkid = scenario.createId("590000822");
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.eventHandler = new TravelTimeEventHandler(departureTimes2travelTimes);
		event.getControler().getEvents().addHandler(this.eventHandler);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int capacity = event.getIteration() * 50 + 1200;
		writeFile(this.departureTimes2travelTimes, capacity);
		this.departureTimes2travelTimes.clear();
		Link link = scenario.getNetwork().getLinks().get(this.linkid);
		link.setCapacity(capacity + 50);
	}

	private void writeFile(SortedMap<Double, Double> departureTimes2travelTimes, int capacity) {
		String outputPath = this.scenario.getConfig().controler().getOutputDirectory();
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath + "/travelTimes_cap_" + capacity + ".txt")));
			bw.write("departureTime \t travelTime");
			bw.newLine();
			for(Double dd : departureTimes2travelTimes.keySet()) {
				bw.write(dd + "\t" + departureTimes2travelTimes.get(dd));
				bw.newLine();
			}
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		// TODO Auto-generated method stub
	}		
}
