package playground.christoph.mobsim;

import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueNode;
import org.matsim.core.mobsim.queuesim.QueueSimEngine;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;

import playground.christoph.events.algorithms.ParallelActEndReplanner;
import playground.christoph.events.algorithms.ParallelLeaveLinkReplanner;

public class MyQueueSimEngine extends QueueSimEngine{

	final private static Logger log = Logger.getLogger(MyQueueSimEngine.class);
	
	protected static boolean actEndReplanning = true; 
	protected static boolean leaveLinkReplanning = true;

	protected Map <NodeImpl, QueueLink[]> lookupTable;
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
			if (driverAgent.getDepartureTime() <= time) 
			{
				PersonImpl person = driverAgent.getPerson();
				personsToReplanActEnd.add(person);
				
				PersonAgent pa = (PersonAgent) driverAgent;
				
//				ActivityImpl fromAct = (Activity)person.getSelectedPlan().getPlanElements().get(pa.getNextActivity() - 2);
				ActivityImpl fromAct = (ActivityImpl)person.getSelectedPlan().getPlanElements().get(pa.getNextActivity());
				fromActActEnd.add(fromAct);
				if (fromAct == null) log.error("Found fromAct that is null!");
				
				vehiclesToReplanActEnd.add(pa.getVehicle());
			}
			
			// it's a priority Queue -> no further Agents will be found
			else break;
		}
			
		if (vehiclesToReplanActEnd.size() > 0)
		{	
//			log.info("Time: " + time  + ", " + vehiclesToReplanActEnd.size() + " vehicles will end their current activity now and need a replanning!");
//			System.out.println("Time: " + time  + ", " + vehiclesToReplanActEnd.size() + " vehicles will end their current activity now and need a replanning!");
			ParallelActEndReplanner.run(fromActActEnd, vehiclesToReplanActEnd, time);
//			log.info("Done parallel Act End Replanning in this step!");
			
			replanningCounter = replanningCounter + vehiclesToReplanActEnd.size();
		}
		
	}	// actEndReplanning

	
	@Override
	protected void simStep(final double time)
	{
		
		// Do Replanning if active...
		if (leaveLinkReplanning) leaveLinkReplanning(time);

	
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
	
	protected void leaveLinkReplanning(double time)
	{
		// Leave Link Replanning Objects
		ArrayList<QueueVehicle> vehiclesToReplanLeaveLink = new ArrayList<QueueVehicle>();
		ArrayList<QueueNode> currentNodesLeaveLink = new ArrayList<QueueNode>();
		
		for (QueueNode queueNode : this.network.getNodes().values())
		{		
			if (queueNode.isActive()) 
			{
				QueueLink[] queueLinks = lookupTable.get(queueNode.getNode());
				
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
							currentNodesLeaveLink.add(queueNode);
						}
					}
										
				}	// for all QueueLink in the Lookup Table
			
			} // if current QueueNode is active
		
		}	// for all QueueNodes in the Network

		if (vehiclesToReplanLeaveLink.size() > 0)
		{
//			ParallelLeaveLinkReplanner.resetLinkTravelTimesLookupTables();
			
//			log.info("Resetting the LinkTravelTime LookupTables in the Replanners. Time: " + time);
			
//			log.info("Found " + vehiclesToReplan.size() + " vehicles that are going to leave their links and need probably a replanning!");
			ParallelLeaveLinkReplanner.run(currentNodesLeaveLink, vehiclesToReplanLeaveLink, time);
//			log.info("Done parallel Leave Link Replanning in this step!");
		}
	
	}	// leaveLinkReplanning
	
	
	/*
	 * Creates a LookupTable that connects a Node with its incoming links
	 */
	public void createLookupTable()
	{
		log.info("start creating table...");
		lookupTable = new TreeMap<NodeImpl, QueueLink[]>();
		for(QueueNode queueNode : this.network.getNodes().values())
		{			
			QueueLink[] queueLinks = new QueueLink[queueNode.getNode().getInLinks().size()];
			
			int index = 0;
			for(LinkImpl link : queueNode.getNode().getInLinks().values())
			{
				queueLinks[index] = this.network.getQueueLink(link.getId());
				
				index++;
			}
			lookupTable.put(queueNode.getNode(), queueLinks);
			
			//QueueNode queueNode = this.getNodes().get(queueLink.getLink().getToNode().getId());
			//lookupTable.put(queueLink, queueNode.getNode());
		}
		log.info("... done");
	}
	
}
