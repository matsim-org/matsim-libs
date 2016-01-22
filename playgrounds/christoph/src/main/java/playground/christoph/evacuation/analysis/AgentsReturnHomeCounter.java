/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsReturnHomeCounter.java
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

package playground.christoph.evacuation.analysis;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;

/**
 * Analyzes when and how agents which do not evacuate return home.
 * 
 * @author cdobler
 */
public class AgentsReturnHomeCounter implements PersonDepartureEventHandler, ActivityStartEventHandler,
		PersonStuckEventHandler, MobsimInitializedListener, IterationEndsListener {

	private static final Logger log = Logger.getLogger(AgentsReturnHomeCounter.class);

	protected int binSize = 60; // by default 1 minute
	protected int nofPictureBins = 36 * 3600 / binSize; // by default 36 hours
	protected int nofBins = 96 * 3600 / binSize; // by default 96 hours

	protected final Scenario scenario;
	protected final Set<String> transportModes;
	protected final CoordAnalyzer coordAnalyzer;
	protected final DecisionDataProvider decisionDataProvider;
	protected final double scaleFactor;

	protected final Map<Id, String> lastTransportModes;
	protected final Map<Id, ActivityInfo> activityInfos;
		
	public AgentsReturnHomeCounter(Scenario scenario, Set<String> transportModes, CoordAnalyzer coordAnalyzer,
			DecisionDataProvider decisionDataProvider, double scaleFactor) {
		this.scenario = scenario;
		this.transportModes = new TreeSet<String>(transportModes);
		this.transportModes.add("none");
		this.coordAnalyzer = coordAnalyzer;
		this.scaleFactor = scaleFactor;
		this.decisionDataProvider = decisionDataProvider;
		
		this.lastTransportModes = new HashMap<Id, String>();
		this.activityInfos = new HashMap<Id, ActivityInfo>();
	}

	/*package*/ static class ActivityInfo {
		double startTime;
		String type;
		Id facilityId;
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		ActivityInfo info = new ActivityInfo();
		info.startTime = event.getTime();
		info.facilityId = event.getFacilityId();
		info.type = event.getActType();
		
		this.activityInfos.put(event.getPersonId(), info);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.lastTransportModes.put(event.getPersonId(), event.getLegMode());
		this.activityInfos.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.lastTransportModes.remove(event.getPersonId());
		this.activityInfos.remove(event.getPersonId());
	}

	@Override
	public void reset(int iteration) {
		lastTransportModes.clear();
		activityInfos.clear();
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		// set initial values in maps
		for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
			
			Id homeFacilityId = this.decisionDataProvider.getHouseholdDecisionData(household.getId()).getHomeFacilityId();
			for (Id personId : household.getMemberIds()) {
				this.lastTransportModes.put(personId, "none");
				
				ActivityInfo info = new ActivityInfo();
				info.startTime = 0.0;
				info.type = "home".intern();
				info.facilityId = homeFacilityId;
				this.activityInfos.put(personId, info);
			}
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		Map<Id, ActivityInfo> map = new TreeMap<Id, ActivityInfo>();
		for (Entry<Id, ActivityInfo> entry : this.activityInfos.entrySet()) {
			Id personId = entry.getKey();
			ActivityInfo info = entry.getValue();
			
			Id householdId = this.decisionDataProvider.getPersonDecisionData(personId).getHouseholdId();
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			EvacuationDecision decision = hdd.getEvacuationDecision();
			
			if (decision == EvacuationDecision.NEVER) {
				Id homeFacilityId = hdd.getHomeFacilityId();
				
				if (!homeFacilityId.equals(info.facilityId)) {
					log.warn("Agent " + personId.toString() + " was expected to be at home (" +
							homeFacilityId.toString() + ") but was found at different location (" +
							info.facilityId.toString() + ")! Agent is ignored!");
					continue;
				}
				
				boolean homeIsAffected = this.coordAnalyzer.isFacilityAffected(((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(homeFacilityId));
				if (homeIsAffected) {
					map.put(personId, info);
				}
			}
		}	

		// write detailed data
		String fileName = null;
		AgentsReturnHomeWriter writer = new AgentsReturnHomeWriter(this.binSize, this.nofPictureBins, event.getIteration());
		
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "affectedAgentsReturnHomeDetail.txt");
		writer.write(fileName, map, this.lastTransportModes);
				
		// create aggregated data - note that data used for detailed output is deleted! 
		Map<String, AtomicInteger[]> data = new TreeMap<String, AtomicInteger[]>();
		data.put("allModes", new AtomicInteger[this.nofBins + 1]);
		for (String transportMode : transportModes) {
			data.put(transportMode, new AtomicInteger[this.nofBins + 1]);
		}
		// initialze arrays
		for (AtomicInteger[] array : data.values()) {
			for (int i = 0; i < array.length; i++) {
				array[i] = new AtomicInteger(0);
			}
		}
		// aggregate data
		for (int currentBin = 0; currentBin <= this.nofBins; currentBin++) {
			int currentTime = currentBin * binSize;

			Iterator<Entry<Id, ActivityInfo>> iter = map.entrySet().iterator();
			
			while (iter.hasNext()) {
				Entry<Id, ActivityInfo> entry = iter.next();
				ActivityInfo info = entry.getValue();
				if (info.startTime <= currentTime) {
					// increase count of used mode
					String transportMode = this.lastTransportModes.get(entry.getKey());
					AtomicInteger[] modeData = data.get(transportMode);
					AtomicInteger count = modeData[currentBin];
					count.addAndGet((int) Math.round(1 * scaleFactor));
					
					// increase total count
					data.get("allModes")[currentBin].addAndGet((int) Math.round(1 * scaleFactor));
					
					// remove entry from map
					iter.remove();
				}
			}
			
			// transfer current values to next bin since we count incrementally
			if (currentBin < this.nofBins) {
				for (AtomicInteger[] array : data.values()) {
					array[currentBin + 1].set(array[currentBin].get());
				}
			}
		}
		
		// write aggregated data
		fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "affectedAgentsReturnHome.txt");
		writer.write(fileName, data);
		
		// write graphs
		String title = "agents not evacuating reached home facility in evacuated area";
		String legend = "agents arrived at home";
		for (String mode : data.keySet()) {
			AtomicInteger[] array = data.get(mode);
			int[] intArray = new int[array.length];
			for (int i = 0; i < array.length; i++) intArray[i] = array[i].get();
				
			fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "affectedAgentsReturnHomeByMode_" + mode + ".png");
			writer.writeGraphic(fileName, title, legend, mode, intArray);
		}
	}

}