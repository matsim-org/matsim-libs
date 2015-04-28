/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
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

package org.matsim.core.mobsim.jdeqsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;

import java.util.HashMap;


/**
 * The starting point of the whole micro-simulation.
 * @see <a href="http://www.matsim.org/docs/jdeqsim">http://www.matsim.org/docs/jdeqsim</a>
 * @author rashid_waraich
 */
public class JDEQSimulation implements Mobsim {

	private final static Logger log = Logger.getLogger(JDEQSimulation.class);

	// READING SIMULATION PARAMETERS FROM CONFIG FILE
	public final static String JDEQ_SIM = "JDEQSim";
	public final static String SQUEEZE_TIME = "squeezeTime";
	public final static String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
	public final static String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
	public final static String MINIMUM_INFLOW_CAPACITY = "minimumInFlowCapacity";
	public final static String CAR_SIZE = "carSize";
	public final static String GAP_TRAVEL_SPEED = "gapTravelSpeed";
	public final static String END_TIME = "endTime";
	// made these public static so I can use them from elsewhere for "config in java". kai, nov'13

	protected Scenario scenario;

	protected final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;

	@Inject
	public JDEQSimulation(final Scenario scenario, final EventsManager events) {
		// constructor

		this.scenario = scenario;
		this.activityDurationInterpretation = 
				this.scenario.getConfig().plans().getActivityDurationInterpretation() ;

		// reset simulation parameters
		SimulationParameters.reset();

		// initialize the events handler to which the micro simulatation gives the events
		SimulationParameters.setProcessEventThread(events);


		Config config = this.scenario.getConfig();
		String squeezeTime = config.findParam(JDEQ_SIM, SQUEEZE_TIME);
		String flowCapacityFactor = config.findParam(JDEQ_SIM, FLOW_CAPACITY_FACTOR);
		String storageCapacityFactor = config.findParam(JDEQ_SIM, STORAGE_CAPACITY_FACTOR);
		String minimumInFlowCapacity = config.findParam(JDEQ_SIM, MINIMUM_INFLOW_CAPACITY);
		String carSize = config.findParam(JDEQ_SIM, CAR_SIZE);
		String gapTravelSpeed = config.findParam(JDEQ_SIM, GAP_TRAVEL_SPEED);
		String endTime = config.findParam(JDEQ_SIM, END_TIME);

		if (squeezeTime != null) {
			SimulationParameters.setSqueezeTime(Double.parseDouble(squeezeTime));
		} else {
			log.info("parameter 'squeezeTime' not defined. Using default value [s]: "
					+ SimulationParameters.getSqueezeTime());
		}

		if (flowCapacityFactor != null) {
			SimulationParameters.setFlowCapacityFactor((Double.parseDouble(flowCapacityFactor)));
		} else {
			log.info("parameter 'flowCapacityFactor' not defined. Using default value: "
					+ SimulationParameters.getFlowCapacityFactor());
		}

		if (storageCapacityFactor != null) {
			SimulationParameters.setStorageCapacityFactor((Double.parseDouble(storageCapacityFactor)));
		} else {
			log.info("parameter 'storageCapacityFactor' not defined. Using default value: "
					+ SimulationParameters.getStorageCapacityFactor());
		}

		if (minimumInFlowCapacity != null) {
			SimulationParameters.setMinimumInFlowCapacity((Double.parseDouble(minimumInFlowCapacity)));
		} else {
			log.info("parameter 'minimumInFlowCapacity' not defined. Using default value [vehicles per hour]: "
					+ SimulationParameters.getMinimumInFlowCapacity());
		}

		if (carSize != null) {
			SimulationParameters.setCarSize((Double.parseDouble(carSize)));
		} else {
			log.info("parameter 'carSize' not defined. Using default value [m]: "
					+ SimulationParameters.getCarSize());
		}

		if (gapTravelSpeed != null) {
			SimulationParameters.setGapTravelSpeed(Double.parseDouble(gapTravelSpeed));
		} else {
			log.info("parameter 'gapTravelSpeed' not defined. Using default value [m/s]: "
					+ SimulationParameters.getGapTravelSpeed());
		}

		if (endTime != null) {
			if (Time.parseTime(endTime)!=0.0){
				SimulationParameters.setSimulationEndTime(Time.parseTime(endTime));
			}
		} else {
			log.info("parameter 'endTime' not defined. Using default value [s]: "
					+ SimulationParameters.getSimulationEndTime());
		}

	}

	@Override
	public void run() {
		Timer t = new Timer();
		t.startTimer();

		Scheduler scheduler = new Scheduler(new MessageQueue());
		SimulationParameters.setAllRoads(new HashMap<Id<Link>, Road>());

		// initialize network
		Road road = null;
		for (Link link : this.scenario.getNetwork().getLinks().values()) {
			road = new Road(scheduler, link);
			SimulationParameters.getAllRoads().put(link.getId(), road);
		}

		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			new Vehicle(scheduler, person, activityDurationInterpretation); // the vehicle registers itself to the scheduler
		}

		scheduler.startSimulation();

		t.endTimer();
		log.info("Time needed for one iteration (only JDEQSimulation part): " + t.getMeasuredTime() + "[ms]");
	}
}
