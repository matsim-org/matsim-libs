/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayControler.java
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

package org.matsim.withinday;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.plans.Person;
import org.matsim.trafficmonitoring.LinkTravelTimeCounter;
import org.matsim.withinday.mobsim.OccupiedVehicle;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.Accident;
import org.matsim.withinday.trafficmanagement.TrafficManagement;
import org.matsim.withinday.trafficmanagement.TrafficManagementConfigParser;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class WithindayControler extends Controler {

	private static final Logger log = Logger.getLogger(WithindayControler.class);

//	private List<WithindayAgent> agents;

	private TrafficManagementConfigParser trafficManagementConfigurator;

	protected WithindayAgentLogicFactory factory;

	protected TrafficManagement trafficManagement;

//	private double lastReplaningTimeStep;

//	private PriorityQueue<WithindayAgent> replanningQueue;

//	private int numberOfReplaningAgents;

	public WithindayControler() {
	}

	/**
	 * @see org.matsim.controler.Controler#startup()
	 */
	@Override
	protected void startup() {
		super.startup();
		LinkTravelTimeCounter.init(this.events, this.network.getLinks().size());
	//initialize the traffic management
		String trafficManagementConfig = this.config.withinday().getTrafficManagementConfiguration();
		if (trafficManagementConfig != null) {
			this.trafficManagementConfigurator = new TrafficManagementConfigParser(this.network, this.events);
			try {
				this.trafficManagementConfigurator.parse(trafficManagementConfig);
			} catch (SAXException e) {
				log.error("An error occured while parsing the trafficmanagement configuration, the traffic management will not be used!");
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				log.error("An error occured while parsing the trafficmanagement configuration, the traffic management will not be used!");
				e.printStackTrace();
			} catch (IOException e) {
				log.error("An error occured while parsing the trafficmanagement configuration, the traffic management will not be used!");
				e.printStackTrace();
			}
		}

	}

	@Override
	protected void setupIteration(final int iteration) {
		super.setupIteration(iteration);
		this.factory = new WithindayAgentLogicFactory(this.network, this.config.charyparNagelScoring());
		if (this.trafficManagementConfigurator != null) {
			this.trafficManagement = this.trafficManagementConfigurator.getTrafficManagement();
			this.trafficManagement.setupIteration(iteration);
		}
	}


	@Override
	protected void runMobSim() {
		List<Integer> withindayIterations = this.config.withinday().getWithindayIterations();
		//check if withinday replanning should be enabled
		if (withindayIterations.contains(Controler.getIteration())) {
			log.info("Starting withinday replanning iteration...");
			//prepare everything to create the agents
			WithindayCreateVehiclePersonAlgorithm vehicleAlgo = new WithindayCreateVehiclePersonAlgorithm(this);

			//build the queuesim
			WithindayQueueSimulation sim = new WithindayQueueSimulation((QueueNetworkLayer)this.network, this.population, this.events, this);
			sim.setVehicleCreateAlgo(vehicleAlgo);
			//set accidents
			if ((this.trafficManagement != null) && !this.trafficManagement.getAccidents().isEmpty())  {
				for (Accident a : this.trafficManagement.getAccidents()) {
					sim.setAccident(a);
				}
			}
			//run the simulation
			sim.run();
	  }
		else {
			super.runMobSim();
		}
	}
	/**
	 * Is currently used to create the WithindayAgent objects with the default belief and desire (intentions are still fixed by
	 * the game theory plans) modules.
	 * Visibility is package as it is called from the WithindayCreateVehiclePersonAlgorithm.
	 * @param person
	 * @param veh
	 */
	void createAgent(final Person person, final OccupiedVehicle veh) {
		WithindayAgent agent = new WithindayAgent(person, veh, this.config.withinday().getAgentVisibilityRange(), this.factory);
		//set the agent's replanning interval
		agent.setReplanningInterval(this.config.withinday().getReplanningInterval());
		//set the contentment threshold
		agent.setReplanningThreshold(this.config.withinday().getContentmentThreshold());
	}


	public void simulationPrepared() {
		this.trafficManagement.simulationPrepared();
	}


	/**
	 * This is delegated from the WithindayQueueSimulation
	 * @param time the current timestep of the QueueSimulation
	 */
	public void beforeSimStep(final double time) {
		this.trafficManagement.updateBeforeSimStrep(time);
	}

	/**
	 * This is delegated from the WithindayQueueSimulation
	 * @param time the current timestep of the QueueSimulation
	 */
	public void afterSimStep(final double time) {

	}

	@Override
	protected void finishIteration(final int iteration) {
		super.finishIteration(iteration);
		this.trafficManagement.finishIteration();
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		new WithindayControler().run(args);
	}


}
