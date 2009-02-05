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

package org.matsim.mobsim.deqsim;

import java.util.HashMap;

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.deqsim.util.Timer;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;

import org.matsim.population.Population;
import org.matsim.population.PopulationReader;

public class DEQSimulation {

	Population population;
	NetworkLayer network;

	public DEQSimulation(final NetworkLayer network, final Population population, final Events events) {
		// constructor

		this.population = population;
		this.network = network;

		// initialize Simulation parameters
		SimulationParameters.setLinkCapacityPeriod(network.getCapacityPeriod());
		// the thread for processing the events
		SimulationParameters.setProcessEventThread( events);

		SimulationParameters.setStuckTime (Double.parseDouble(Gbl.getConfig().getParam("simulation",
				"stuckTime")));
		SimulationParameters.setFlowCapacityFactor( Double.parseDouble(Gbl.getConfig().getParam("simulation",
				"flowCapacityFactor")));
		SimulationParameters.setStorageCapacityFactor ( Double.parseDouble(Gbl.getConfig().getParam(
				"simulation", "storageCapacityFactor")));

		// allowed testing to hook in here
		if (SimulationParameters.getTestEventHandler() != null) {
			SimulationParameters.getProcessEventThread().addHandler(SimulationParameters.getTestEventHandler());
		}

		if (SimulationParameters.getTestPlanPath() != null) {
			// read population
			Population pop = new Population(Population.NO_STREAMING);
			PopulationReader plansReader = new MatsimPopulationReader(pop);
			plansReader.readFile(SimulationParameters.getTestPlanPath());

			this.population = pop;

		}

		if (SimulationParameters.getTestPopulationModifier() != null) {
			this.population = SimulationParameters.getTestPopulationModifier().modifyPopulation(this.population);
		}

	}

	public void run() {
		Timer t = new Timer();
		t.startTimer();

		Scheduler scheduler = new Scheduler();
		SimulationParameters.setAllRoads (new HashMap<String, Road>());

		// initialize network
		Road road = null;
		for (Link link : network.getLinks().values()) {
			road = new Road(scheduler, link);
			SimulationParameters.getAllRoads().put(link.getId().toString(), road);
		}

		// initialize vehicles
		Vehicle vehicle = null;
		// the vehicle has registered itself to the scheduler
		for (Person person : this.population.getPersons().values()) {
			vehicle = new Vehicle(scheduler, person);
		}
		
		// just inserted to remove message in bug analysis, that vehicle variable is never read
		vehicle.toString();

		scheduler.startSimulation();

		t.endTimer();
		t.printMeasuredTime("Time needed for one iteration (only DES part): ");

	}
}
