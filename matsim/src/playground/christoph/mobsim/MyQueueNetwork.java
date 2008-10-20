/* *********************************************************************** *
 * project: org.matsim.*
 * MyQueueNetwork.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.controler.Controler;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNetworkFactory;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.events.algorithms.ParallelLeaveLinkReplanner;
import playground.christoph.knowledge.nodeselection.ParallelCreateKnownNodesMap;

public class MyQueueNetwork extends QueueNetwork{
	
	protected Controler controler; 
	
	final private static Logger log = Logger.getLogger(MyQueueNetwork.class);
	
	public MyQueueNetwork(NetworkLayer networkLayer)
	{
		super(networkLayer);
	}
	
	public MyQueueNetwork(NetworkLayer networkLayer, QueueNetworkFactory<QueueNode, QueueLink> factory) {
		super(networkLayer, factory);
	}

	/**
	 * Implements one simulation step, called from simulation framework
	 * @param time The current time in the simulation.
	 */
	@Override
	protected void simStep(final double time) {
		
		ArrayList<Vehicle> vehiclesToReplan = new ArrayList<Vehicle>();
		ArrayList<QueueNode> currentNodes = new ArrayList<QueueNode>();
		
		//Map<Id, QueueNode> queueNodeMap = this.getNodes();
		Map<Id, QueueLink> queueLinkMap = this.getLinks();
	
		for (QueueLink link : queueLinkMap.values()) 
		{
			//if(link instanceof QueueLink)
			//{
				// LeaveLinkReplanning
				Queue<Vehicle> vehiclesQueue = link.getVehiclesInBuffer();
								
				QueueNode queueNode = this.getNodes().get(link.getLink().getToNode().getId());
								
				for (Vehicle vehicle : vehiclesQueue) 
				{	
					// check if leave link replanning flag is set
					boolean replanning = (Boolean)vehicle.getDriver().getPerson().getCustomAttributes().get("leaveLinkReplanning");
					if (replanning)
					{
						vehiclesToReplan.add(vehicle);
						currentNodes.add(queueNode);
					}
				}				
			//}
		}
		
		if (vehiclesToReplan.size() > 0)
		{
//			log.info("Found " + vehiclesToReplan.size() + " vehicles that are going to leave their links and need probably a replanning!");
			ParallelLeaveLinkReplanner.run(currentNodes, vehiclesToReplan, time);
//			log.info("Done parallel replanning in this step!");
		}
		
		super.simStep(time);
	}
	
	public void setControler(Controler controler) 
	{
		this.controler = controler;
	}
	
	public Controler getControler()
	{
		return controler;
	}

}
