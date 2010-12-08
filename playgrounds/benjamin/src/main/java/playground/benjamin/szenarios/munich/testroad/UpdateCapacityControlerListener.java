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
	private SortedMap<Id, Double> personId2travelTimesPerIteration = new TreeMap<Id, Double>();
	private SortedMap<Integer, SortedMap<Id, Double>> travelTimes = new TreeMap<Integer, SortedMap<Id, Double>>();
	private SortedMap<Id, Double> personId2enterTimesPerIteration = new TreeMap<Id, Double>();
	private SortedMap<Integer, SortedMap<Id, Double>> enterTimes = new TreeMap<Integer, SortedMap<Id, Double>>();
	
	private Scenario scenario;
	private Integer capacity;
	private Id linkLeaveId;
	private Id linkEnterId;
	private Integer stepSize;

	public UpdateCapacityControlerListener(Scenario scenario, String linkLeaveId, String linkEnterId, int startCapacity, int stepSize) {
		this.scenario = scenario;
		this.linkLeaveId = scenario.createId(linkLeaveId);
		this.linkEnterId = scenario.createId(linkEnterId);
		this.capacity = startCapacity;
		this.stepSize = stepSize;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.eventHandler = new TravelTimeEventHandler(personId2travelTimesPerIteration, personId2enterTimesPerIteration, linkLeaveId, linkEnterId);
		event.getControler().getEvents().addHandler(this.eventHandler);
		
		Link link = scenario.getNetwork().getLinks().get(this.linkLeaveId);
		link.setCapacity(this.capacity);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
//		writeTravelTimes(this.personId2travelTimesPerIteration);
		addTravelTimesToTable(this.personId2travelTimesPerIteration);
		this.personId2travelTimesPerIteration.clear();
		
		addEnterTimesToTable(this.personId2enterTimesPerIteration);
		this.personId2enterTimesPerIteration.clear();
		
		capacity = this.capacity + this.stepSize;
		Link link = scenario.getNetwork().getLinks().get(this.linkLeaveId);
		link.setCapacity(capacity);
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		writeTable(this.travelTimes, "x_travelTimes");
		writeTable(this.enterTimes, "x_enterTimes");
	}

	private void addEnterTimesToTable(SortedMap<Id, Double> personId2enterTimesPerIteration) {
		SortedMap<Id, Double> temp = new TreeMap<Id, Double>();
		for(Id personId : personId2enterTimesPerIteration.keySet()){
			Double enterTime = personId2enterTimesPerIteration.get(personId);
			
			temp.put(personId , enterTime);
		}
		enterTimes.put(capacity, temp);
	}

	private void addTravelTimesToTable(SortedMap<Id, Double> personId2travelTimesPerIteration) {
		SortedMap<Id, Double> temp = new TreeMap<Id, Double>();
			for(Id personId : personId2travelTimesPerIteration.keySet()){
				Double travelTime = personId2travelTimesPerIteration.get(personId);
				
				temp.put(personId , travelTime);
			}
			travelTimes.put(capacity, temp);
	}

	private void writeTable(SortedMap<Integer, SortedMap<Id, Double>> table, String outputName) {
		String outputPath = this.scenario.getConfig().controler().getOutputDirectory() + "/";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath + outputName + ".txt")));
			//header
			bw.write("personId" + "\t");
			for(int iteration : table.keySet()){
				if(iteration == table.lastKey()){
					bw.write("cap" + iteration);
				}
				else{
					bw.write("cap" + iteration + "\t");
				}
			}
			bw.newLine();
			
			//fill with values
			SortedMap<Id, Double> firstCapacity = table.get(table.firstKey());
			for(Id personId : firstCapacity.keySet()){
				
				/*not too elegant and a bit dangerous --
				but there is writeTable(enterTime) 
				to compare real enter time (from personId)
				with simulated enter time*/
				String personId2Split = personId.toString();
				String [] array = personId2Split.split("t");
				bw.write(array[0] + "\t");
				
				for(int iteration : table.keySet()){
					Double time = table.get(iteration).get(personId);
					if(iteration == table.lastKey()){
						bw.write(time.toString());
					}
					else{
						bw.write(time + "\t");
					}
				}
				bw.newLine();
			}
			bw.close();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}		
}