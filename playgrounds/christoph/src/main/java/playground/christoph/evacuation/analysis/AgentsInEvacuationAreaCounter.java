/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInEvacuationAreaCounter.java
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

package playground.christoph.evacuation.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.scenario.ScenarioImpl;

import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;

/**
 * Counts the number of agents within an evacuated area.
 * The count is split up into transport modes and activity performing.
 * 
 * @author cdobler
 */
public class AgentsInEvacuationAreaCounter implements LinkEnterEventHandler,
		AgentArrivalEventHandler, AgentDepartureEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		AgentStuckEventHandler, IterationEndsListener,
		MobsimInitializedListener {

	private static final Logger log = Logger.getLogger(AgentsInEvacuationAreaCounter.class);

	protected int binSize = 300; // by default 5 minutes
	protected int nofBins = 36 * 3600 / binSize; // by default 36 hours
	protected int currentBin = 0; // current time bin slot

	protected final Scenario scenario;
	protected final Set<String> transportModes;
	protected final CoordAnalyzer coordAnalyzer;
	protected final double scaleFactor;
	
	protected Set<Id> activityAgentsInEvacuationArea;
	protected Map<String, Set<Id>> legAgentsInEvacuationArea;
	protected Map<Id, String> currentTransportMode;
	
	protected Set<Id> vehiclesWithoutDriver;
	protected Map<Id, Id> driverVehicleMap;
	protected Map<Id, List<Id>> vehiclePassengersMap;	// the driver is not included!
	
	protected Map<String, int[]> legBins;
	protected int[] activityBins;

	public AgentsInEvacuationAreaCounter(Scenario scenario, Set<String> transportModes, CoordAnalyzer coordAnalyzer, double scaleFactor) {
		this.scenario = scenario;
		this.transportModes = transportModes;
		this.coordAnalyzer = coordAnalyzer;
		this.scaleFactor = scaleFactor;
		
		init();
	}

	private void init() {
		currentTransportMode = new HashMap<Id, String>();
		activityAgentsInEvacuationArea = new HashSet<Id>();
		legAgentsInEvacuationArea = new HashMap<String, Set<Id>>();
		vehiclesWithoutDriver = new HashSet<Id>();
		driverVehicleMap = new HashMap<Id, Id>();
		vehiclePassengersMap = new HashMap<Id, List<Id>>();
		activityBins = new int[nofBins];
		legBins = new TreeMap<String, int[]>();

		for (String string : transportModes) {
			legAgentsInEvacuationArea.put(string, new HashSet<Id>());
			legBins.put(string, new int[nofBins]);
		}
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (Person person : scenario.getPopulation().getPersons().values()) {

			if (person.getSelectedPlan().getPlanElements().size() == 0) continue;
			else {
				Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);

				boolean isAffected = false;
				if (activity.getFacilityId() != null) {
					ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(activity.getFacilityId());
					isAffected = this.coordAnalyzer.isFacilityAffected(facility);
				} else {
					Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
					isAffected = this.coordAnalyzer.isLinkAffected(link);
					log.warn("No facility defined in activity - taking coordinate from activity...");
				}

				if (isAffected) activityAgentsInEvacuationArea.add(person.getId());
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		updateBinData(event.getTime());

		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());

		String transportMode = currentTransportMode.get(event.getPersonId());
		Set<Id> set = legAgentsInEvacuationArea.get(transportMode);

		Id vehicleId = driverVehicleMap.get(event.getPersonId());
		List<Id> passengers = null;
		Set<Id> passengerSet = null;
		if (vehicleId != null) {
			passengers = vehiclePassengersMap.get(vehicleId);
			passengerSet = legAgentsInEvacuationArea.get(PassengerDepartureHandler.passengerTransportMode);
		}
		
		if (this.coordAnalyzer.isLinkAffected(link)) {
			set.add(event.getPersonId());
			if (passengers != null) {
				for (Id passengerId : passengers) passengerSet.add(passengerId);
			}
		} else {
			set.remove(event.getPersonId());
			if (passengers != null) {
				for (Id passengerId : passengers) passengerSet.remove(passengerId);
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		updateBinData(event.getTime());

		String transportMode = currentTransportMode.get(event.getPersonId());
		Set<Id> set = legAgentsInEvacuationArea.get(transportMode);
		set.remove(event.getPersonId());

		currentTransportMode.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		updateBinData(event.getTime());

		boolean isAffected = false;
		if (event.getFacilityId() != null) {
			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(event.getFacilityId());
			isAffected = this.coordAnalyzer.isFacilityAffected(facility);
		} else {
			Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
			isAffected = this.coordAnalyzer.isLinkAffected(link);
			log.warn("No facilityId given - using link coordinates!");
		}

		if (isAffected) {
			activityAgentsInEvacuationArea.add(event.getPersonId());
		} else {
			activityAgentsInEvacuationArea.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		updateBinData(event.getTime());

		activityAgentsInEvacuationArea.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		updateBinData(event.getTime());

		String transportMode = event.getLegMode();
		currentTransportMode.put(event.getPersonId(), transportMode);

		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
		Set<Id> set = legAgentsInEvacuationArea.get(transportMode);
		if (this.coordAnalyzer.isLinkAffected(link)) {
			set.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
		updateBinData(event.getTime());

		String transportMode = event.getLegMode();
		/*
		 * When called from the cleanup method of the QSim, the transportMode is
		 * not set.
		 */
		if (transportMode != null) {
			Set<Id> set = legAgentsInEvacuationArea.get(transportMode);
			set.remove(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		/*
		 * We create a list where the passengers will be added.
		 * The driver is NOT added to the list!
		 */
		List<Id> passengers = vehiclePassengersMap.get(event.getVehicleId());
		if (passengers == null) {
			passengers = new ArrayList<Id>();
			vehiclePassengersMap.put(event.getVehicleId(), passengers);
		}
		
		boolean isDriver = currentTransportMode.get(event.getPersonId()).equals(TransportMode.car);
		if (isDriver) {
			driverVehicleMap.put(event.getPersonId(), event.getVehicleId());
		} else {			
			passengers.add(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		List<Id> passengers = vehiclePassengersMap.get(event.getVehicleId());
		
		// try to remove the person as driver
		Id driverId = driverVehicleMap.remove(event.getPersonId());
		
		// check whether the agent is the driver of the vehicle
		boolean isDriver;
		if (driverId == null) isDriver = false;
		else isDriver = true;
		
		if (isDriver) {	
			// if the vehicle is empty
			if (passengers.size() == 0) {
				vehiclePassengersMap.remove(event.getVehicleId());
				driverVehicleMap.remove(driverId);
			}
			// the driver leaves the car but some passengers are left
			else {
				vehiclesWithoutDriver.add(driverId);
				driverVehicleMap.remove(driverId);
			}
		}
		// it is a passenger
		else {
			// remove it from the list
			passengers.remove(event.getPersonId());
			
			// if it is the last passenger and the driver has also already left the vehicle
			if (passengers.size() == 0 && vehiclesWithoutDriver.contains(event.getVehicleId())) {
				vehiclesWithoutDriver.remove(event.getVehicleId());
				vehiclePassengersMap.remove(event.getVehicleId());
			}
		}
	}

	@Override
	public void reset(int iteration) {
		currentBin = 0;
		activityAgentsInEvacuationArea.clear();
		vehiclesWithoutDriver.clear();
		driverVehicleMap.clear();
		vehiclePassengersMap.clear();
		activityBins = new int[nofBins];
		
		for (String string : transportModes) {
			legAgentsInEvacuationArea.put(string, new HashSet<Id>());
			legBins.put(string, new int[nofBins]);
		}
		
		for (Set<Id> set : legAgentsInEvacuationArea.values()) {
			set.clear();
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// for (Id id : activityAgentsInEvacuationArea) {
		// log.info(id.toString());
		// }

		// ensure, that the last bin is written
		updateBinData(currentBin + 1);

		String fileName = null;

		AgentsInEvacuationAreaWriter writer = new AgentsInEvacuationAreaWriter(this.binSize, event.getIteration());

		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea.txt");
		writer.write(fileName, activityBins, legBins);

		// write activity performing graph
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_activity.png");
		writer.writeGraphic(fileName, "activity", activityBins);

		// write leg performing graphs
		for (String legMode : transportModes) {
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_" + legMode + ".png");
			writer.writeGraphic(fileName, legMode, legBins.get(legMode));
		}

		// write all agents into a graph
		int[] allAgents = activityBins.clone();
		for (String legMode : transportModes) {
			int[] leg = legBins.get(legMode);
			for (int i = 0; i < allAgents.length; i++) {
				allAgents[i] = allAgents[i] + leg[i];
			}
			fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_allAgents.png");
			writer.writeGraphic(fileName, "all agents", allAgents);
		}

		// write single graph with all agents
		String[] names = new String[transportModes.size() + 2];
		int[][] data = new int[transportModes.size() + 2][];
		names[0] = "all";
		data[0] = allAgents;
		names[1] = "activity";
		data[1] = activityBins;
		int i = 2;
		for (String legMode : transportModes) {
			data[i] = legBins.get(legMode);
			names[i] = legMode;
			i++;
		}
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "agentsInEvacuationArea_comparison.png");
		writer.writeGraphic(fileName, names, data);
	}

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	/*
	 * If the given time is in a later time bin, we can calculate the values for
	 * the past time bins.
	 */
	private void updateBinData(double time) {
		int binIndex = getBinIndex(time);
		if (binIndex > currentBin) {

			// for all not processed past bins
			for (int index = currentBin; index < binIndex; index++) {
				activityBins[index] = (int) Math.round(activityAgentsInEvacuationArea.size() * scaleFactor);
				for (String string : transportModes) {
					legBins.get(string)[index] = (int) Math.round(legAgentsInEvacuationArea.get(string).size() * scaleFactor);
				}
			}

		}

		currentBin = binIndex;
	}
}
