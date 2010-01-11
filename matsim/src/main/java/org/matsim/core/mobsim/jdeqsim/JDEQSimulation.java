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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.jdeqsim.util.Timer;
import org.matsim.core.utils.misc.Time;


/**
 * The starting point of the whole micro-simulation.
 * @see http://www.matsim.org/docs/jdeqsim
 * @author rashid_waraich
 */
public class JDEQSimulation {

	protected static Logger log = null;
	protected Population population;
	protected Network network;
	

	public JDEQSimulation(final Network network, final Population population, final EventsManager events) {
		// constructor
		
		log = Logger.getLogger(JDEQSimulation.class);

		this.population = population;
		this.network = network;

		// reset simulation parameters
		SimulationParameters.reset();
		
		// initialize the events handler to which the micro simulatation gives the events
		SimulationParameters.setProcessEventThread(events);


		// READING SIMULATION PARAMETERS FROM CONFIG FILE
		final String JDEQ_SIM = "JDEQSim";
		final String SQUEEZE_TIME = "squeezeTime";
		final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
		final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
		final String MINIMUM_INFLOW_CAPACITY = "minimumInFlowCapacity";
		final String CAR_SIZE = "carSize";
		final String GAP_TRAVEL_SPEED = "gapTravelSpeed";
		final String END_TIME = "endTime";

		Config config = Gbl.getConfig();
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

	public void run() {
		Timer t = new Timer();
		t.startTimer();

		Scheduler scheduler = new Scheduler(new MessageQueue());
		SimulationParameters.setAllRoads(new HashMap<Id, Road>());

		// initialize network
		Road road = null;
		for (Link link : this.network.getLinks().values()) {
			road = new Road(scheduler, link);
			SimulationParameters.getAllRoads().put(link.getId(), road);
		}

		// initialize vehicles
		Vehicle vehicle = null;
		// the vehicle has registered itself to the scheduler
		for (Person person : this.population.getPersons().values()) {
			vehicle = new Vehicle(scheduler, person);
		}

		// just inserted to remove message in bug analysis, that vehicle
		// variable is never read
		vehicle.toString();

		scheduler.startSimulation();

		t.endTimer();
		log.info("Time needed for one iteration (only JDEQSimulation part): " + t.getMeasuredTime() + "[ms]");
	}
}
