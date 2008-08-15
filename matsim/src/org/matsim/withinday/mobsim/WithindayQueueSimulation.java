/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayQueueSimulation.java
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

package org.matsim.withinday.mobsim;

import org.matsim.events.Events;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.withinday.WithindayAgent;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

/**
 * This extension of the QueueSimulation is used for withinday replanning. It contains functionality
 * to trigger the replanning of the WithindayAgents and provides methods to set an accident, i.e. a
 * capacity change of a link.
 *
 * @author dgrether
 *
 */
public class WithindayQueueSimulation extends QueueSimulation {

	private WithindayControler controler;

	private TrafficManagement trafficManagement;

	public WithindayQueueSimulation(final NetworkLayer net,
			final Population plans, final Events events, final WithindayControler controler) {
		super(net, plans, events);
		this.controler = controler;
		this.setVehiclePrototye(OccupiedVehicle.class);
	}

	@Override
	protected void addVehicleToLink(final Vehicle veh) {
		super.addVehicleToLink(veh);
		createAgent(veh.getDriver(), (OccupiedVehicle)veh);
	}

	/**
	 * Is currently used to create the WithindayAgent objects with the default belief and desire (intentions are still fixed by
	 * the game theory plans) modules.
	 * @param person
	 * @param veh
	 */
	private void createAgent(final Person person, final OccupiedVehicle veh) {
		WithindayAgent agent = new WithindayAgent(person, veh, controler.getConfig().withinday().getAgentVisibilityRange(), this.controler.getAgentLogicFactory());
		//set the agent's replanning interval
		agent.setReplanningInterval(controler.getConfig().withinday().getReplanningInterval());
		//set the contentment threshold
		agent.setReplanningThreshold(controler.getConfig().withinday().getContentmentThreshold());
	}


	@Override
	protected void prepareSim() {
	  super.prepareSim();
		this.trafficManagement.simulationPrepared();
	}

	@Override
	protected void beforeSimStep(final double time) {
		super.beforeSimStep(time);
  	//check capacity change whishes for pending items
		this.trafficManagement.updateBeforeSimStep(time);
	}


	public void setTrafficManagement(TrafficManagement trafficManagement) {
		this.trafficManagement = trafficManagement;
	}






}
