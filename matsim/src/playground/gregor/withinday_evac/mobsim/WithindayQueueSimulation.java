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

package playground.gregor.withinday_evac.mobsim;

import org.matsim.events.Events;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;

import playground.gregor.withinday_evac.communication.InformationExchanger;

/**
 * This extension of the QueueSimulation is used for withinday replanning. It contains functionality
 * to trigger the replanning of the WithindayAgents and provides methods to set an accident, i.e. a
 * capacity change of a link.
 *
 * @author dgrether
 *
 */
public class WithindayQueueSimulation extends QueueSimulation {

	private final playground.gregor.withinday_evac.controler.WithindayControler controler;
	private final InformationExchanger informationExchanger;


	public WithindayQueueSimulation(final NetworkLayer net,
			final Population plans, final Events events, final playground.gregor.withinday_evac.controler.WithindayControler withindayControler) {
		super(net, plans, events);

		final QueueNetwork qNet = new QueueNetwork(net);
		this.network = qNet;
		this.controler = withindayControler;
		this.informationExchanger = new InformationExchanger(net);
		super.setAgentFactory(new WithindayAgentFactory(this.informationExchanger, this.network.getNetworkLayer()));
		
	}
	
	





}
