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

import java.util.Date;

import org.matsim.controler.Controler;
import org.matsim.events.Events;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Population;


public class ReplanningQueueSimulation extends QueueSimulation{
	
	protected Controler controler;
	
	public ReplanningQueueSimulation(final NetworkLayer network, final Population plans, final Events events)
	{
		super(network, plans, events);
		
		// eigenes Queuenetwork hinterlegen, welches MyQueueNodes enthält -> nötig für das Replanning!
		this.network = new MyQueueNetwork(network, new MyQueueNetworkFactory());
		this.networkLayer = network;	
		setAgentFactory(new MyAgentFactory());
	}
	
	
	public QueueNetwork getQueueNetwork()
	{
		return network;
	}
	
	public void setControler(Controler controler)
	{
		this.controler = controler;

		// Referenz auf den Controler mit dem Replanning Algorithmus hinterlegen
		((MyQueueNetwork)this.network).setControler(controler);
	}
	
	public Controler getControler()
	{
		return controler;
	}

}
