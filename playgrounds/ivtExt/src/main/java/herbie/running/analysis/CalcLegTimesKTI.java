/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesKTI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package herbie.running.analysis;

import herbie.running.population.algorithms.AbstractClassifiedFrequencyAnalysis;
import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * Calculates average trip durations by mode.
 *
 * @author meisterk
 *
 */
public class CalcLegTimesKTI extends AbstractClassifiedFrequencyAnalysis implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private TreeMap<Id, Boolean> agentPerformsPtInteraction = new TreeMap<Id, Boolean>();
	private TreeMap<Id, Boolean> agentPerformsAnyPt = new TreeMap<Id, Boolean>();
	private TreeMap<Id, Double> ptPerformingTime = new TreeMap<Id, Double>();
	private String standardPtMode = "standardPt";
	private String onlyPtWalk = "onlyPtWalk";
	private ArrayList<Id> agentsPerformingAnyPt = new ArrayList<Id>();
	private ArrayList<Id> agentsPerformingPtInteraction = new ArrayList<Id>();
	private TreeMap<Id, Id> agentStartingLinkId = new TreeMap<Id, Id>();
	
	private boolean timeWindowIsSet = false;
	private double startTime;
	private double endTime;
	

	public CalcLegTimesKTI(Population pop, PrintStream out) {
		super(out);
		this.population = pop;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
		this.agentStartingLinkId.put(event.getPersonId(), event.getLinkId());
	}

	@Override
	public void reset(int iteration) {
		this.rawData.clear();
		this.frequencies.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
		
		
		if (depTime != null && agent != null) {
			
			Id personId = event.getPersonId();
			double travelTime = event.getTime() - depTime;
			String mode = event.getLegMode();
			
			if(mode.equals("transit_walk")||mode.equals("pt")){
				this.agentPerformsAnyPt.put(personId, true);
				
				if(this.ptPerformingTime.containsKey(personId)){
					travelTime = travelTime + this.ptPerformingTime.get(personId);
				}
				this.ptPerformingTime.put(personId, travelTime);
			}
			else{
				Frequency frequency = null;
				ResizableDoubleArray rawData = null;
				if (!this.frequencies.containsKey(mode)) {
					frequency = new Frequency();
					this.frequencies.put(mode, frequency);
					rawData = new ResizableDoubleArray();
					this.rawData.put(mode, rawData);
				} else {
					frequency = this.frequencies.get(mode);
					rawData = this.rawData.get(mode);
				}
				if(travelTime >= 0.0 && eventIsInTimeWindow(event.getTime())){
					frequency.addValue(travelTime);
					rawData.addElement(travelTime);
				}
			}
		}
	}

	public TreeMap<String, Double> getAverageTripDurationsByMode() {
		TreeMap<String, Double> averageTripDurations = new TreeMap<String, Double>();
		for (String mode : this.rawData.keySet()) {
			averageTripDurations.put(mode, StatUtils.mean(this.rawData.get(mode).getElements()));
		}
		return averageTripDurations;
	}

	public double getAverageOverallTripDuration() {

		double overallTripDuration = 0.0;
		int overallNumTrips = 0;

		for (String mode : this.rawData.keySet()) {
			overallTripDuration += StatUtils.sum(this.rawData.get(mode).getElements());
			overallNumTrips += this.rawData.get(mode).getNumElements();
		}

		return (overallTripDuration / overallNumTrips);
	}

	@Override
	public void run(Person person) {
		// not used
	}

	// This used to listen to ActivityEvents, which were ActivityStartEvent and ActivityEndEvent.
	// So I assume this sould be executed for ActivityStartEvents and ActivityEndEvents, although it seems
	// strange. michaz 2012
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals("pt interaction")) {
			this.agentPerformsPtInteraction.put(event.getPersonId(), true);
		}
		else{
			if(this.agentPerformsAnyPt.containsKey(event.getPersonId())){
				Id personId = event.getPersonId();
				double time = event.getTime();
				handleActivityStartOrEnd(personId, time);
			}
		}
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals("pt interaction")) {
			this.agentPerformsPtInteraction.put(event.getPersonId(), true);
		}
		else{
			if(this.agentPerformsAnyPt.containsKey(event.getPersonId())){
				Id personId = event.getPersonId();
				double time = event.getTime();
				handleActivityStartOrEnd(personId, time);
			}
		}
	}

	private void handleActivityStartOrEnd(Id personId, double time) {
		String mode;
		if(this.agentPerformsPtInteraction.containsKey(personId)){
			mode = standardPtMode;
		}
		else{
			mode = onlyPtWalk;
		}
		
		Frequency frequency = null;
		ResizableDoubleArray rawData = null;
		if (!this.frequencies.containsKey(mode)) {
			frequency = new Frequency();
			this.frequencies.put(mode, frequency);
			rawData = new ResizableDoubleArray();
			this.rawData.put(mode, rawData);
		} else {
			frequency = this.frequencies.get(mode);
			rawData = this.rawData.get(mode);
		}
		
		if(this.ptPerformingTime.get(personId) >= 0.0 && eventIsInTimeWindow(time)){
			frequency.addValue(this.ptPerformingTime.get(personId));
			rawData.addElement(this.ptPerformingTime.get(personId));
		}
		
		this.agentPerformsAnyPt.remove(personId);
		this.agentPerformsPtInteraction.remove(personId);
		this.ptPerformingTime.remove(personId);
	}
	

	

	private boolean eventIsInTimeWindow(double time) {
		
		if(!timeWindowIsSet) return true;
		
		if(time > startTime && time < endTime) return true;
		
		else return false;
	}
	
	public void setTimeWindow(double startTime, double endTime){
		
		this.startTime = startTime;
		this.endTime = endTime;
		this.timeWindowIsSet = true;
	}

}