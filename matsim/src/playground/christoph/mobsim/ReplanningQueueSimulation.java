/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeQueueSimulation.java
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

package playground.christoph.mobsim;

import org.matsim.core.api.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkLayer;


public class ReplanningQueueSimulation extends QueueSimulation{

	protected Controler controler;

	public ReplanningQueueSimulation(final NetworkLayer network, final Population plans, final Events events)
	{
		super(network, plans, events);

		// eigenes Queuenetwork hinterlegen, welches MyQueueNodes enthaelt -> noetig fuer das Replanning!
		this.network = new MyQueueNetwork(network, new MyQueueNetworkFactory());
		this.networkLayer = network;
		setAgentFactory(new MyAgentFactory(this));
	}


	public QueueNetwork getQueueNetwork()
	{
		return this.network;
	}

	public void setControler(final Controler controler)
	{
		this.controler = controler;

		// Referenz auf den Controler mit dem Replanning Algorithmus hinterlegen
		((MyQueueNetwork)this.network).setControler(controler);
	}

	public Controler getControler()
	{
		return this.controler;
	}

}
