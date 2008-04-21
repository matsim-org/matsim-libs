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

import java.util.PriorityQueue;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.mobsim.QueueLink;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.trafficmanagement.Accident;
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

	private PriorityQueue<CapacityChangeEvent> capacityEvents = new PriorityQueue<CapacityChangeEvent>();

	private TrafficManagement trafficManagement;

	public WithindayQueueSimulation(final NetworkLayer net,
			final Plans plans, final Events events, final WithindayControler controler) {
		super(net, plans, events);
		this.controler = controler;
		this.setVehiclePrototye(OccupiedVehicle.class);
	}

	@Override
	protected void prepareSim() {
	  super.prepareSim();
		this.trafficManagement.simulationPrepared();
	}

	@Override
	public void beforeSimStep(final double time) {
		super.afterSimStep(time);
  	//check capacity change whishes for pending items
		doCapacityChanges(time);
		this.trafficManagement.updateBeforeSimStep(time);
	}


	public void setTrafficManagement(TrafficManagement trafficManagement) {
		this.trafficManagement = trafficManagement;
		for (Accident a : this.trafficManagement.getAccidents()) {
			setAccident(a);
		}
	}

	/**
	 * Generating an accident (capacity reduction) on a certain link over a
	 * certain time interval
	 *
	 * @param a
	 */
	public void setAccident(final Accident a) {
		QueueLink accidentLink = this.network.getQueueLink(new IdImpl(a.getLinkId()));
		this.capacityEvents.add(new CapacityChangeEvent(a.getStartTime(), accidentLink, a.getCapacityReductionFactor()));
		this.capacityEvents.add(new CapacityChangeEvent(a.getEndTime(), accidentLink, 1/a.getCapacityReductionFactor()));
	}

	private void doCapacityChanges(final double time) {
		while ((this.capacityEvents.size() != 0) && (this.capacityEvents.peek().getTime() < time)) {
			CapacityChangeEvent event = this.capacityEvents.poll();
			event.getLink().changeSimulatedFlowCapacity(event.getCapacityScaleFactor());
		}
	}




}
