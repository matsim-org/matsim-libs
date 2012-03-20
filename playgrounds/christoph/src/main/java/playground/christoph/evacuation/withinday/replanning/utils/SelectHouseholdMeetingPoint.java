/* *********************************************************************** *
 * project: org.matsim.*
 * SelectHouseholdMeetingPoint.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.withinday.replanning.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.withinday.replanning.modules.ReplanningModule;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.VehiclesTracker;

/**
 * Decides where a household will meet after the evacuation order has been given.
 * This could be either at home or at another location, if the home location is
 * not treated to be secure. However, households might meet at their insecure home
 * location and then evacuate as a unit.
 * 
 * By default, all households meet at home and the select another location, if
 * their home location is not secure.
 * 
 * @author cdobler
 */
public class SelectHouseholdMeetingPoint implements SimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(SelectHouseholdMeetingPoint.class);
	
	private final Scenario scenario;
	private final ReplanningModule replanningModule;
	private final HouseholdsTracker householdsTracker;
	private final VehiclesTracker vehiclesTracker;
	private final CoordAnalyzer coordAnalyzer;

	private final int numOfThreads;
	
	private Thread[] threads;
	private Runnable[] runnables;
	
	public SelectHouseholdMeetingPoint(Scenario scenario, ReplanningModule replanningModule,
			HouseholdsTracker householdsTracker, VehiclesTracker vehiclesTracker, CoordAnalyzer coordAnalyzer) {
		this.scenario = scenario;
		this.replanningModule = replanningModule;
		this.householdsTracker = householdsTracker;
		this.vehiclesTracker = vehiclesTracker;
		this.coordAnalyzer = coordAnalyzer;
		
		this.numOfThreads = this.scenario.getConfig().global().getNumberOfThreads();
	}
	
	/*
	 * So far, households will directly meet at a rescue facility or
	 * at their home facility. For the later case, they have to select
	 * a next meeting point, when all household members have arrived
	 * at their current meeting point. At the moment, this next meeting 
	 * point is hard coded as a rescue facility.
	 * 
	 * So far, there is only a single rescue facility.
	 * Instead, multiple *real* rescue facilities could be defined. 
	 */
	public Id selectNextMeetingPoint(Id householdId) {
		Id rescueMeetingPointId = scenario.createId("rescueFacility");
		this.householdsTracker.getHouseholdPosition(householdId).setMeetingPointFacilityId(rescueMeetingPointId);
		return rescueMeetingPointId;
	}

	public Id getMeetingPoint(Id householdId) {
		return this.householdsTracker.getHouseholdPosition(householdId).getMeetingPointFacilityId();
	}
	
	/*
	 * If the evacuation starts in the current time step, define the
	 * household meeting points.
	 */
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		double time = e.getSimulationTime();
		if (time == EvacuationConfig.evacuationTime) initThreads(time);
	}
	
	private void initThreads(double time) {
		threads = new Thread[this.numOfThreads];
		runnables = new SelectHouseholdMeetingPointRunner[this.numOfThreads];
		
		for (int i = 0; i < this.numOfThreads; i++) {
			
			SelectHouseholdMeetingPointRunner runner = new SelectHouseholdMeetingPointRunner(scenario, 
					replanningModule, householdsTracker, vehiclesTracker, coordAnalyzer.createInstance());
			runner.setTime(time);
			runnables[i] = runner; 
					
			Thread thread = new Thread(runner);
			thread.setDaemon(true);
			thread.setName(SelectHouseholdMeetingPointRunner.class.toString() + i);
			threads[i] = thread;
		}
		
		// assign households to threads
		int roundRobin = 0;
		Households households = ((ScenarioImpl) scenario).getHouseholds();
		for (Household household : households.getHouseholds().values()) {
			// ignore empty households
			if (household.getMemberIds().size() == 0) continue;
			
			((SelectHouseholdMeetingPointRunner) runnables[roundRobin % this.numOfThreads]).addHouseholdToCheck(household);
			roundRobin++;
		}
		
		// start threads
		for (Thread thread : threads) thread.start();
		
		// wait for the threads to finish
		try {
			for (Thread thread : threads) {
				thread.join();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		/*
		 * Print some statistics
		 */
		int meetAtHome = 0;
		int meetAtRescue = 0;
		int meetSecure = 0;
		int meetInsecure = 0;
		
		for (Household household : households.getHouseholds().values()) {
			// ignore empty households
			if (household.getMemberIds().size() == 0) continue;
			
			HouseholdPosition householdPosition = this.householdsTracker.getHouseholdPosition(household.getId());
			if (householdPosition.getHomeFacilityId().equals(householdPosition.getMeetingPointFacilityId())) meetAtHome++;
			else meetAtRescue++;
			
			ActivityFacility meetingFacility = ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().get(householdPosition.getMeetingPointFacilityId());
			if (this.coordAnalyzer.isFacilityAffected(meetingFacility)) meetInsecure++;
			else meetSecure++;
		}
		
		log.info("Households meet at home facility:   " + meetAtHome);
		log.info("Households meet at rescue facility: " + meetAtRescue);
		log.info("Households meet at secure place:   " + meetSecure);
		log.info("Households meet at insecure place: " + meetInsecure);
	}
}
