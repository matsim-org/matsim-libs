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
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.controler.Controler;
import org.matsim.mobsim.queuesim.PersonAgent;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueNetworkFactory;
import org.matsim.mobsim.queuesim.QueueNode;
import org.matsim.mobsim.queuesim.Vehicle;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;

import playground.christoph.events.algorithms.ParallelActEndReplanner;
import playground.christoph.events.algorithms.ParallelLeaveLinkReplanner;

public class MyQueueNetwork extends QueueNetwork{
	
	protected Controler controler; 
	
	protected static boolean leaveLinkReplanning = true;
	protected static boolean actEndReplanning = true; 
	
	final private static Logger log = Logger.getLogger(MyQueueNetwork.class);
	protected Map <Node, QueueLink[]> lookupTable;
	
	public MyQueueNetwork(NetworkLayer networkLayer)
	{
		super(networkLayer);
		
		createLookupTable();
	}
	
	public MyQueueNetwork(NetworkLayer networkLayer, QueueNetworkFactory<QueueNode, QueueLink> factory) {
		super(networkLayer, factory);
		
		createLookupTable();
	}

	/*
	 * Creates a lookuptable that connects 
	 */
	public void createLookupTable()
	{
		log.info("start creating table...");
		lookupTable = new TreeMap<Node, QueueLink[]>();
		for(QueueNode queueNode : this.getNodes().values())
		{			
			QueueLink[] queueLinks = new QueueLink[queueNode.getNode().getInLinks().size()];
			
			int index = 0;
			for(Link link : queueNode.getNode().getInLinks().values())
			{
				queueLinks[index] = this.getQueueLink(link.getId());
				
				index++;
			}
			lookupTable.put(queueNode.getNode(), queueLinks);
			
			//QueueNode queueNode = this.getNodes().get(queueLink.getLink().getToNode().getId());
			//lookupTable.put(queueLink, queueNode.getNode());
		}
		log.info("... done");
	}
	
	@Override
	protected void simStep(final double time) {
		
		// Leave Link Replanning Objects
		ArrayList<Vehicle> vehiclesToReplanLeaveLink = new ArrayList<Vehicle>();
		ArrayList<QueueNode> currentNodesLeaveLink = new ArrayList<QueueNode>();
		
		for (QueueNode queueNode : this.getNodes().values())
		{		
			if (queueNode.isActive()) 
			{
				QueueLink[] queueLinks = lookupTable.get(queueNode.getNode());
				
				for (int i = 0; i < queueLinks.length; i++)
				{
					Queue<Vehicle> inBufferVehiclesQueue = queueLinks[i].getVehiclesInBuffer();
					
					for (Vehicle vehicle : inBufferVehiclesQueue) 
					{	
						// check if Leave Link Replanning flag is set
						boolean leaveLinkReplanning = (Boolean)vehicle.getDriver().getPerson().getCustomAttributes().get("leaveLinkReplanning");
						if (leaveLinkReplanning)
						{
							vehiclesToReplanLeaveLink.add(vehicle);
							currentNodesLeaveLink.add(queueNode);
						}
					}
										
				}	// for all QueueLink in the Lookup Table
			
			} // if current QueueNode is active
		
		}	// for all QueueNodes in the Network

		
		if (vehiclesToReplanLeaveLink.size() > 0)
		{
//			log.info("Found " + vehiclesToReplan.size() + " vehicles that are going to leave their links and need probably a replanning!");
			ParallelLeaveLinkReplanner.run(currentNodesLeaveLink, vehiclesToReplanLeaveLink, time);
//			log.info("Done parallel replanning in this step!");
		}
		
		
		// Act End Replanning Objects
		ArrayList<Vehicle> vehiclesToReplanActEnd = new ArrayList<Vehicle>();
		ArrayList<Act> fromActActEnd = new ArrayList<Act>();
		
		/*
		 * Checking if the current QueueNode is active is not allowed here!
		 * A Node may be inactive when an Agent ends his Activity at one of the
		 * In-Links of the Node what will reactivate that Node. This would be to
		 * late for us, so we have to check EVERY QueueNode for Agents that will
		 * end their current Activity now.  
		 */
		for (QueueNode queueNode : this.getNodes().values())
		{	
			QueueLink[] queueLinks = lookupTable.get(queueNode.getNode());
				
			for (int i = 0; i < queueLinks.length; i++)
			{
				/*
				 * For every Vehicle in the Parking List of the QueueLink is checked,
				 * if the current time is after the planned departure time. If true,
				 * the vehicle is going to end it's current activity in this SimStep,
				 * so Act End Replanning is needed.
				 */
				PriorityQueue<Vehicle> onParkingListVehiclesQueue = queueLinks[i].getVehiclesOnParkingList();
				
				boolean test = true;
				
				for (Vehicle vehicle : onParkingListVehiclesQueue)
				{	
					// check if Act End Replanning flag is set
					boolean actEndReplanning = (Boolean)vehicle.getDriver().getPerson().getCustomAttributes().get("endActivityReplanning");
					if(actEndReplanning) 
					{						
						// if the current Activity has ended
						if (vehicle.getDepartureTime_s() <= time) 
						{
							if (test == false)
							{
								log.error("\"Test\" is false but should be true ?!");
							}
							
							vehiclesToReplanActEnd.add(vehicle);
							
							PersonAgent personAgent = vehicle.getDriver();
							Act fromAct = (Act)personAgent.getActsLegs().get(personAgent.getNextActivity() - 2);
							fromActActEnd.add(fromAct);
						}
						
						/*
						 * onParkingListVehiclesQueue is a PriorityQueue which is sorted by the departure time
						 * of its Vehicles. So if a vehicle has not reached its departure time the is no need to
						 * check the following vehicles.
						 */
						else
						{	test = false;
//							break;
						}
					}
				}
					
			}	// for all QueueLink in the Lookup Table
		
		}	// for all QueueNodes in the Network
		
	
		if (vehiclesToReplanActEnd.size() > 0)
		{	
			log.info(vehiclesToReplanActEnd.size() + " vehicles will end their current activity now and need a replanning!");
			ParallelActEndReplanner.run(fromActActEnd, vehiclesToReplanActEnd, time);
		}
		
		
		// ... and finally execute the Simulation Step.
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
