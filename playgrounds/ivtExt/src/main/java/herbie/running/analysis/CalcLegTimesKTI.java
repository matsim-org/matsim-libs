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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.ActivityEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.ActivityEventHandler;

/**
 * Calculates average trip durations by mode.
 *
 * @author meisterk
 *
 */
public class CalcLegTimesKTI extends AbstractClassifiedFrequencyAnalysis implements AgentDepartureEventHandler, AgentArrivalEventHandler, ActivityEventHandler {

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
	

	public CalcLegTimesKTI(Population pop, PrintStream out) {
		super(out);
		this.population = pop;
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
		this.agentStartingLinkId.put(event.getPersonId(), event.getLinkId());
	}

	@Override
	public void reset(int iteration) {
		this.rawData.clear();
		this.frequencies.clear();
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
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
				if(travelTime >= 0.0){
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

	@Override
	public void handleEvent(ActivityEvent event) {
		if (event.getActType().equals("pt interaction")) {
			this.agentPerformsPtInteraction.put(event.getPersonId(), true);
		}
		else{
			if(this.agentPerformsAnyPt.containsKey(event.getPersonId())){
				
				Id personId = event.getPersonId();
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
				
				if(this.ptPerformingTime.get(personId) >= 0.0){
					frequency.addValue(this.ptPerformingTime.get(personId));
					rawData.addElement(this.ptPerformingTime.get(personId));
				}
				
				this.agentPerformsAnyPt.remove(personId);
				this.agentPerformsPtInteraction.remove(personId);
				this.ptPerformingTime.remove(personId);
			}
		}
	}
}