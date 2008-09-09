/* *********************************************************************** *
 * project: org.matsim.*
 * MyQueueNode.java
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

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.Node;

public class MyQueueNode extends QueueNode{
	
	final private static Logger log = Logger.getLogger(MyQueueNode.class);
	
	public MyQueueNode(Node n, QueueNetwork queueNetwork)
	{
		super(n, queueNetwork);
	}

	// ////////////////////////////////////////////////////////////////////
	// Queue related movement code
	// ////////////////////////////////////////////////////////////////////
	public boolean moveVehicleOverNode(final Vehicle veh, final double now) 
	{
		new Replanner(this, veh, now);
		
		// doReplanning here!
	//	knowledgeReplaner.reset(1);
//		QueueSimulation.getEvents().processEvent(new AgentReplanEvent(now, veh.getDriver().getPerson(), this.link, veh.getCurrentLeg()));
		
		return super.moveVehicleOverNode(veh, now);
	}
	
	protected Controler getControler()
	{
	if(this.queueNetwork instanceof MyQueueNetwork)
		{
			return ((MyQueueNetwork)this.queueNetwork).getControler();
		}
		else
			log.error("Could not return a Controler!");
			return null;
	}
}
