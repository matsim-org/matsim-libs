package playground.christoph.mobsim;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueNode;
import org.matsim.core.mobsim.queuesim.QueueSimEngine;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.christoph.events.algorithms.ParallelActEndReplanner;
import playground.christoph.events.algorithms.ParallelLeaveLinkReplanner;
import playground.christoph.network.MyLinkImpl;

public class MyQueueSimEngine extends QueueSimEngine{

	final private static Logger log = Logger.getLogger(MyQueueSimEngine.class);
	
	protected static boolean actEndReplanning = true; 
	protected static boolean leaveLinkReplanning = true;

	//protected Map <NodeImpl, QueueLink[]> lookupTable;actEndReplanning
	protected Map <Id, QueueLink[]> lookupTable;
	protected QueueNetwork network;
	protected ReplanningQueueSimulation simulation;
	
	public static int replanningCounter = 0;
	
	public MyQueueSimEngine(final QueueNetwork network, final Random random)
	{
		super(network, random);
		
		this.network = network;
		createLookupTable();
	}
		
	public static void doActEndReplanning(boolean value)
	{
		actEndReplanning = value;
	}
	
	public static boolean isActEndReplanning()
	{
		return actEndReplanning;
	}
	
	public static void doLeaveLinkReplanning(boolean value)
	{
		leaveLinkReplanning = value;
	}
	
	public static boolean isLeaveLinkReplanning()
	{
		return leaveLinkReplanning;
	}

	public void setQueueSimulation(ReplanningQueueSimulation queueSimulation)
	{
		this.simulation = queueSimulation;
	}
	
	public void actEndReplanning(double time)
	{
		Date start = new Date();
		
		// Act End Replanning Objects
		ArrayList<QueueVehicle> vehiclesToReplanActEnd = new ArrayList<QueueVehicle>();
		ArrayList<PersonImpl> personsToReplanActEnd = new ArrayList<PersonImpl>();
		ArrayList<ActivityImpl> fromActActEnd = new ArrayList<ActivityImpl>();
		/*
		 * Checking only Links that leed to active Nodes is not allowed here!
		 * If a Person enters an inative Link, this Link is reactivated - but
		 * this is done when simulating the SimStep, what is to late for us.
		 * Checking if the current QueueNode is active is not allowed here!
		 * So we check every Link within the QueueNetwork.
		 */

		PriorityBlockingQueue<DriverAgent> queue = simulation.getActivityEndsList();
		
		for (DriverAgent driverAgent : queue)
		{			
			// If the Agent will depart
			if (driverAgent.getDepartureTime() <= time)
			{
				// Skip Agent if Replanning Flag is not set
				boolean replanning = (Boolean)driverAgent.getPerson().getCustomAttributes().get("endActivityReplanning");
				if(!replanning) continue; 
				
				PersonImpl person = driverAgent.getPerson();
				personsToReplanActEnd.add(person);
				
				PersonAgent pa = (PersonAgent) driverAgent;
				
				// worked in V1.0 - looks like some changes in the indices happend since then
//				ActivityImpl fromAct = (Activity)person.getSelectedPlan().getPlanElements().get(pa.getNextActivity() - 2);
				
				// seems to work currently but uses deprecated Method...
//				ActivityImpl fromAct = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(pa.getNextActivity());
		
				// New approach using non deprecated Methods
				// The Person is currently at an Activity and is going to leave it.
				// The Person's CurrentLeg should point to the leg that leads to that Activity...
				List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
				
				Leg leg = pa.getCurrentLeg();
				
				ActivityImpl fromAct = null;
				
				// first Activity is running - there is no previous Leg
				if (leg == null)
				{
					fromAct = (ActivityImpl)planElements.get(0);
				}
				else
				{
					int index = planElements.indexOf(leg);
					// If the leg is part of the Person's plan
					if (index >= 0)
					{
						fromAct = (ActivityImpl)planElements.get(index + 1);
					}
				}
								
				if (fromAct == null)
				{
					log.error("Found fromAct that is null!");
				}
				else
				{
					fromActActEnd.add(fromAct);
				}
				
				vehiclesToReplanActEnd.add(pa.getVehicle());
			}
			
			// it's a priority Queue -> no further Agents will be found
			else break;
		}
			
		if (vehiclesToReplanActEnd.size() > 0)
		{	
//			log.info("Time: " + time  + ", " + vehiclesToReplanActEnd.size() + " vehicles will end their current activity now and need a replanning!");
//			System.out.println("Time: " + time  + ", " + vehiclesToReplanActEnd.size() + " vehicles will end their current activity now and need a replanning!");
//			Date startReplanning = new Date();

			new ParallelActEndReplanner().run(fromActActEnd, vehiclesToReplanActEnd, time);
//			ParallelActEndReplanner.run(fromActActEnd, vehiclesToReplanActEnd, time);
			
//			log.info("Done parallel Act End Replanning in this step!");
			
			replanningCounter = replanningCounter + vehiclesToReplanActEnd.size();
			
//			long totalTime = new Date().getTime() - start.getTime();
//			long replanningTime = new Date().getTime() - startReplanning.getTime();
//			double ratio = Double.valueOf(replanningTime) / Double.valueOf(totalTime);
//			log.info("Total Time: " + totalTime + ", Replanning Time: " + replanningTime + ", Ratio: " + ratio);
		}
	}	// actEndReplanning

	
	@Override
	protected void simStep(final double time)
	{
		// Do Replanning if active...
//		log.info("Do LeaveLinkReplanning...");
		if (leaveLinkReplanning) leaveLinkReplanning(time);
//		if (leaveLinkReplanning) leaveLinkReplanningOld(time);
//		log.info("done");
		
/* 
 * If using ActivityEndsList from the QueueSimulation, the Replanning has to be started 
 * from the ReplanningQueueSimulation because doing it from here would be already to late.
 * (The handleActivityEnds Method from the QueueSimulation starts before and kicks the
 * Agents out of the List).
 */
//		if (actEndReplanning) actEndReplanning(time);
		
		
/*
		if (actEndReplanning) 
		{
			long now = System.currentTimeMillis();
			actEndReplanning(time);
			long duration = System.currentTimeMillis() - now;
			log.info("actEndReplanning V1: " + duration);
			
			now = System.currentTimeMillis();
			actEndReplanningV2(time);
			duration = System.currentTimeMillis() - now;
			log.info("actEndReplanning V2: " + duration);
		}
*/		
		// ... and finally execute the Simulation Step.
		super.simStep(time);	
	}
		
	protected void leaveLinkReplanningOld(double time)
	{
		Date start = new Date();
		
		// Leave Link Replanning Objects
		ArrayList<QueueVehicle> vehiclesToReplanLeaveLink = new ArrayList<QueueVehicle>();
//		ArrayList<QueueNode> currentNodesLeaveLink = new ArrayList<QueueNode>();
		
		for (QueueNode queueNode : this.network.getNodes().values())
		{		
			if (queueNode.isActive()) 
			{
				//QueueLink[] queueLinks = lookupTable.get(queueNode.getNode());
				QueueLink[] queueLinks = lookupTable.get(queueNode.getNode().getId());
				
				for (int i = 0; i < queueLinks.length; i++)
				{
					Queue<QueueVehicle> inBufferVehiclesQueue = queueLinks[i].getVehiclesInBuffer();
					
					for (QueueVehicle vehicle : inBufferVehiclesQueue) 
					{	
						// check if Leave Link Replanning flag is set
						boolean leaveLinkReplanning = (Boolean)vehicle.getDriver().getPerson().getCustomAttributes().get("leaveLinkReplanning");
						if (leaveLinkReplanning)
						{
							vehiclesToReplanLeaveLink.add(vehicle);
//							currentNodesLeaveLink.add(queueNode);
						}
					}
										
				}	// for all QueueLink in the Lookup Table
			
			} // if current QueueNode is active
		
		}	// for all QueueNodes in the Network

		if (vehiclesToReplanLeaveLink.size() > 0)
		{
//			ParallelLeaveLinkReplanner.resetLinkTravelTimesLookupTables();
			
//			log.info("Resetting the LinkTravelTime LookupTables in the Replanners. Time: " + time);
			
			Date startReplanning = new Date();
			
//			log.info("Found " + vehiclesToReplan.size() + " vehicles that are going to leave their links and need probably a replanning!");
//			ParallelLeaveLinkReplanner.run(currentNodesLeaveLink, vehiclesToReplanLeaveLink, time);
			new ParallelLeaveLinkReplanner().run(vehiclesToReplanLeaveLink, time);
			
			replanningCounter = replanningCounter + vehiclesToReplanLeaveLink.size();
			
			long totalTime = new Date().getTime() - start.getTime();
			long replanningTime = new Date().getTime() - startReplanning.getTime();
			double ratio = Double.valueOf(replanningTime) / Double.valueOf(totalTime);
//			log.info("Total Time: " + totalTime + ", Replanning Time: " + replanningTime + ", Ratio: " + ratio + ", Vehicles: " + vehiclesToReplanLeaveLink.size());
			
//			log.info("Done parallel Leave Link Replanning in this step!");
		}
	
	}	// leaveLinkReplanning
	
	protected void leaveLinkReplanning(double time)
	{
		ArrayList<QueueVehicle> vehiclesToReplanLeaveLink = (ArrayList<QueueVehicle>)((MyQueueNetwork)this.network).getLinkReplanningMap().getReplanningVehicles(time);
		if (vehiclesToReplanLeaveLink.size() > 0)
		{
			replanningCounter = replanningCounter + vehiclesToReplanLeaveLink.size();
			new ParallelLeaveLinkReplanner().run(vehiclesToReplanLeaveLink, time);
		}
	}
	
	/*
	 * Creates a LookupTable that connects a Node with its incoming links
	 */
	public void createLookupTable()
	{
		log.info("start creating table...");
		//lookupTable = new TreeMap<NodeImpl, QueueLink[]>();
		lookupTable = new TreeMap<Id, QueueLink[]>();
		for(QueueNode queueNode : this.network.getNodes().values())
		{			
			QueueLink[] queueLinks = new QueueLink[queueNode.getNode().getInLinks().size()];
			
			int index = 0;
			for(LinkImpl link : queueNode.getNode().getInLinks().values())
			{
				queueLinks[index] = this.network.getQueueLink(link.getId());
				
				index++;
			}
			//lookupTable.put(queueNode.getNode(), queueLinks);
			lookupTable.put(queueNode.getNode().getId(), queueLinks);
			
			//QueueNode queueNode = this.getNodes().get(queueLink.getLink().getToNode().getId());
			//lookupTable.put(queueLink, queueNode.getNode());
		}
		log.info("... done");
	}
	
}
