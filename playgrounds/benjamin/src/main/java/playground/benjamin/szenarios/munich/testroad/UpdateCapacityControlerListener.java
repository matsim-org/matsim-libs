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
	private SortedMap<Double, Double> activityEndTimes2travelTimesPerIteration = new TreeMap<Double, Double>();
	private SortedMap<Integer, SortedMap<Double, Double>> tableTotal = new TreeMap<Integer, SortedMap<Double, Double>>();
	private Scenario scenario;
	private Integer capacity;
	private Id linkId;
	private Id testVehicleActivityLinkId;
	private Integer stepSize;

	public UpdateCapacityControlerListener(Scenario scenario, String linkId, String testVehicleActivityLinkId, int startCapacity, int stepSize) {
		this.scenario = scenario;
		this.linkId = scenario.createId(linkId);
		this.testVehicleActivityLinkId = scenario.createId(testVehicleActivityLinkId);
		this.capacity = startCapacity;
		this.stepSize = stepSize;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.eventHandler = new TravelTimeEventHandler(activityEndTimes2travelTimesPerIteration, linkId, testVehicleActivityLinkId);
		event.getControler().getEvents().addHandler(this.eventHandler);
		
		Link link = scenario.getNetwork().getLinks().get(this.linkId);
		link.setCapacity(this.capacity);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		writeTravelTimes(this.activityEndTimes2travelTimesPerIteration);
		addTravelTimesToTableTotal(this.activityEndTimes2travelTimesPerIteration);
		this.activityEndTimes2travelTimesPerIteration.clear();
		
		capacity = this.capacity + this.stepSize;
		Link link = scenario.getNetwork().getLinks().get(this.linkId);
		link.setCapacity(capacity);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writeAllTravelTimes(this.tableTotal);
	}

	private void addTravelTimesToTableTotal(SortedMap<Double, Double> activityEndTimes2travelTimesPerIteration) {
		SortedMap<Double, Double> temp = new TreeMap<Double, Double>();
			for(Double activityEndTime : activityEndTimes2travelTimesPerIteration.keySet()){
				Double travelTime = activityEndTimes2travelTimesPerIteration.get(activityEndTime);
				
				temp.put(activityEndTime , travelTime);
			}
			tableTotal.put(capacity, temp);
	}

	private void writeTravelTimes(SortedMap<Double, Double> activityEndTimes2travelTimes) {
		String outputPath = this.scenario.getConfig().controler().getOutputDirectory();
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath + "/travelTimes_cap_" + capacity + ".txt")));
			bw.write("activityEndTime \t travelTime");
			bw.newLine();
			for(Double dd : activityEndTimes2travelTimes.keySet()) {
				bw.write(dd + "\t" + activityEndTimes2travelTimes.get(dd));
				bw.newLine();
			}
			bw.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeAllTravelTimes(SortedMap<Integer, SortedMap<Double, Double>> tableTotal) {
		String outputPath = this.scenario.getConfig().controler().getOutputDirectory();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath + "/travelTimes" + ".txt")));
			//header
			bw.write("activityEndTime" + "\t");
			for(int iteration : tableTotal.keySet()){
				if(iteration == tableTotal.lastKey()){
					bw.write("cap" + iteration);
				}
				else{
					bw.write("cap" + iteration + "\t");
				}
			}
			bw.newLine();
			
			//fill with values
			SortedMap<Double, Double> firstCapacity = tableTotal.get(tableTotal.firstKey());
			for(Double activityEndTime : firstCapacity.keySet()){
				bw.write(activityEndTime + "\t");
				for(int iteration : tableTotal.keySet()){
					Double travelTime = tableTotal.get(iteration).get(activityEndTime);
					if(iteration == tableTotal.lastKey()){
						bw.write(travelTime.toString());
					}
					else{
						bw.write(travelTime + "\t");
					}
				}
				bw.newLine();
			}
			bw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}		
}
