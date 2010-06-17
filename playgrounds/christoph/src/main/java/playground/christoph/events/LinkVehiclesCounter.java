package playground.christoph.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.NetworkLayer;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;
import org.matsim.ptproject.qsim.netsimengine.QLinkInternalI;
import org.matsim.ptproject.qsim.netsimengine.QNetwork;

import playground.christoph.network.MyLinkImpl;

public class LinkVehiclesCounter implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler,
		AgentDepartureEventHandler, AgentWait2LinkEventHandler,
		AgentStuckEventHandler, SimulationAfterSimStepListener,
		SimulationInitializedListener {

	/*
	 * Counts the number of Vehicles on the QueueLinks of a given QueueNetwork.
	 * 
	 * Additional a List of Links with changed VehicleCount per TimeStep is
	 * created.
	 */

	private QNetwork qNetwork;

	private static final Logger log = Logger.getLogger(LinkVehiclesCounter.class);

	// cyclical Status information - adapted from QueueSimulation
	protected static final int INFO_PERIOD = 3600;

	Map<Id, Integer> waitingMap;
	Map<Id, Integer> vehQueueMap;
	Map<Id, Integer> parkingMap;
	// Map<Id, Integer> transitVehicleStopQueueMap; // maybe later...
	Map<Id, Integer> bufferMap;

	/*
	 *  List of Links where the number of driving Cars has changed in the 
	 *  current TimeStep (and maybe changed back again...)
	 *  Is used internally and changed during a SimStep
	 */
	Map<Id, Integer> countChangedMap; // LinkId, CarCount driving on the Link
	
	/*
	 * List of Links where the Number of driving has changed in the last 
	 * TimeStep. It can for example be used by Replanners between two SimSteps
	 * to check for which Links they have to recalculate the TravelTimes and
	 * TravelCosts.
	 */
	Map<Id, Integer> countChangedInTimeStepMap; // Counts from the previous TimeStep
	
	Map<Id, Integer> countLastTimeStepMap; // Counts from the previous TimeStep
	
	int lostVehicles;
	int initialVehicleCount;

	private AgentCounterI agentCounter;

	public void setQueueNetwork(QNetwork qNetwork)
	{
		this.qNetwork = qNetwork;

		// Doing this will create the Maps and fill them with "0" Entries.
//		createInitialCounts();
	}

	private synchronized void createInitialCounts() {
		
		// initialize the Data Structures
		parkingMap = new HashMap<Id, Integer>();
		waitingMap = new HashMap<Id, Integer>();
		vehQueueMap = new HashMap<Id, Integer>();
		bufferMap = new HashMap<Id, Integer>();

		countChangedMap = new HashMap<Id, Integer>();
		countChangedInTimeStepMap = new HashMap<Id, Integer>();
		countLastTimeStepMap = new HashMap<Id, Integer>();
		
		initialVehicleCount = 0;
		lostVehicles = 0;
		
		// collect the Counts
		for (QLinkInternalI qLink : qNetwork.getLinks().values()) {
			int vehCount = qLink.getAllVehicles().size();

			initialVehicleCount = initialVehicleCount + vehCount;

			Id id = qLink.getLink().getId();
			parkingMap.put(id, vehCount);
			waitingMap.put(id, 0);
			vehQueueMap.put(id, 0);
			bufferMap.put(id, 0);

			// initially every LinkCount has changed
			countChangedMap.put(id, 0);
			
			// same value in the TimeStep before
			countLastTimeStepMap.put(id, 0);
		}
	}

	public synchronized void handleEvent(LinkEnterEvent event) {
//		log.info("LinkEnterEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = vehQueueMap.get(id);
		vehCount++;
		vehQueueMap.put(id, vehCount);

		int count;
		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
		else count = countLastTimeStepMap.get(id);
		
		countChangedMap.put(id, count + 1);
	}

	public synchronized void handleEvent(LinkLeaveEvent event) {
//		log.info("LinkLeaveEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = bufferMap.get(id);
		vehCount--;
		bufferMap.put(id, vehCount);

		int count;
		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
		else count = countLastTimeStepMap.get(id);
		
		countChangedMap.put(id, count - 1);
	}

	public synchronized void handleEvent(AgentArrivalEvent event) {
//		log.info("AgentArrivalEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = vehQueueMap.get(id);
		vehCount--;
		vehQueueMap.put(id, vehCount);

		vehCount = parkingMap.get(id);
		vehCount++;
		parkingMap.put(id, vehCount);

		int count;
		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
		else count = countLastTimeStepMap.get(id);
		
		countChangedMap.put(id, count - 1);
	}

	/*
	 * Structure of this method: Have a look at
	 * QueueSimulation.agentDeparts()...
	 */
	public synchronized void handleEvent(AgentDepartureEvent event) {
//		log.info("AgentDepartureEvent " + event.getLinkId().toString() + " " + event.getTime());

		// Handling depends on the Route of the Agent
		TransportMode legMode = event.getLegMode();

		if (legMode.equals(TransportMode.car)) {
			Id id = event.getLinkId();

			int vehCount;
			vehCount = parkingMap.get(id);
			vehCount--;
			parkingMap.put(id, vehCount);
			
			/*
			 * This is the "else" part from below. Looks like as it works 
			 * correct with that, even if i have no idea why :? 
			 */
			vehCount = waitingMap.get(id);
			vehCount++;
			waitingMap.put(id, vehCount);

			int count;
			if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
			else count = countLastTimeStepMap.get(id);
			
			countChangedMap.put(id, count + 1);
			
			/*
			 * The QueueSimulation additionally checks "&& agent.chooseNextLink() == null"
			 * The agent seems to be still null here - it is set after the event was created :(
			 */
//			NetworkRoute route = (NetworkRoute) leg.getRoute();
//			
//			LinkImpl link = queueNetwork.getNetworkLayer().getLink(event.getLinkId());
//			QueueVehicle vehicle = queueNetwork.getQueueLink(event.getLinkId()).getVehicle(route.getVehicleId());
//			DriverAgent agent = vehicle.getDriver();
//
//			if (route.getEndLink() == link && agent.chooseNextLink() == null)
//			{
//				// nothing to do here... ArrivalEvent is created and that is handled elsewere.
//			} 
//			else
//			{
//				vehCount = waitingMap.get(id);
//				vehCount++;
//				waitingMap.put(id, vehCount);
//
//				countChangedMap.put(id, vehCount);
//			}
		} else {
			log.warn("Unknown Leg Mode!");
		}
	}

	public synchronized void handleEvent(AgentWait2LinkEvent event) {
//		log.info("AgentWait2LinkEvent " + event.getLinkId().toString() + " " + event.getTime());

		Id id = event.getLinkId();

		int vehCount;
		vehCount = waitingMap.get(id);
		vehCount--;
		waitingMap.put(id, vehCount);

		vehCount = bufferMap.get(id);
		vehCount++;
		bufferMap.put(id, vehCount);
	}

	public synchronized void handleEvent(AgentStuckEvent event) {
//		log.info("AgentStuckEvent " + event.getLinkId().toString() + " " + event.getTime());

		lostVehicles++;

		// Nothing else to do here - if a Vehicles is stucked, it is removed
		// and a LeaveLinkEvent is created!
		
//		Id id = event.getLinkId();
//		
//		int count;
//		if (countChangedMap.containsKey(id)) count = countChangedMap.get(id);
//		else count = countLastTimeStepMap.get(id);
//		
//		// where to remove it?
//		countChangedMap.put(id, count - 1);
	}

	/*
	 * Looks like that there is a small Error in the counting of the VehQueue
	 * Map (and only in this Map what is quite strange because i have no idea
	 * how this is possible)
	 * 
	 * To use this method, some getters have to be added to the QueueLink Class!
	 */
	
	private synchronized void checkVehicleCount(SimulationAfterSimStepEvent e)
	{  
//		log.info("checking Vehicle Count");
//		if (e.getSimulationTime() >= infoTime) 
//		{ 
//			infoTime += INFO_PERIOD;
//			log.info("SIMULATION AT " + Time.writeTime(e.getSimulationTime()) + " checking parking Vehicles Counts");
//		}
	 
		for (QLinkInternalI qLink : qNetwork.getLinks().values()) 
		{
			Id id = qLink.getLink().getId();
	 
			/*
			 * Vehicles can be moved from the Vehicle Queue to the Buffer
			 * without creating an Event, so we don't know the exact counts
			 * in the single lists. Due to the fact that sum must be correct
			 * we can check the sum or move the vehicles in our list as long
			 * as our count is wrong. 
			 */
//			int inBufferCount = queueLink.getVehiclesInBuffer().size();
//			int myInBufferCount = bufferMap.get(id); 
//			
//			if (inBufferCount != myInBufferCount)
//			{ 
//				int diff = inBufferCount - myInBufferCount;
//				bufferMap.put(id, inBufferCount);
//	 
//				int myVehQueueCount = vehQueueMap.get(queueLink.getLink().getId());
//				vehQueueMap.put(id, myVehQueueCount - diff);
//			}
	 
			if (this.agentCounter.getLost() != lostVehicles)
			{ 
				log.error("Wrong LostCount");
				log.error("Expected: " + this.agentCounter.getLost() + ", Found: " + lostVehicles);
			} 
//			
//			if (queueLink.parkingCount() != parkingMap.get(id))
//			{
//				log.error("Wrong ParkingCount on Link " + id); log.error("Expected: " + queueLink.parkingCount() + ", Found: " + parkingMap.get(id)); 
//			} 
//			
//			if (queueLink.bufferCount() != bufferMap.get(id))
//			{
//				log.error("Wrong BufferCount on Link " + id); log.error("Expected: " + queueLink.bufferCount() + ", Found: " + bufferMap.get(id)); 
//			} 
//			
//			if (queueLink.vehQueueCount() != vehQueueMap.get(id))
//			{
//				log.error("Wrong VehicleQueueCount on Link " + id);
//				log.error("Expected: " + queueLink.vehQueueCount() + ", Found: " + vehQueueMap.get(id)); 
//			} 
//			
//			if (queueLink.waitingCount() != waitingMap.get(id)) 
//			{ 
//				log.error("Wrong WaitingCount on Link " + id);
//				log.error("Expected: " + queueLink.waitingCount() + ", Found: " + waitingMap.get(id));
//			}
			
			int allVehicles = parkingMap.get(id) + bufferMap.get(id) + vehQueueMap.get(id) + waitingMap.get(id);
			if (qLink.getAllVehicles().size() != allVehicles)
			{
				log.error("Wrong VehicleCount on Link " + id); log.error("Expected: " + qLink.getAllVehicles().size() + ", Found: " + allVehicles); 
			} 	
		}
		
		int parking = 0;
		int waiting = 0;
		int inQueue = 0;
		int inBuffer = 0;
	 
		for (int count : parkingMap.values()) parking = parking + count; 
		for (int count : waitingMap.values()) waiting = waiting + count; 
		for (int count : vehQueueMap.values()) inQueue = inQueue + count; 
		for (int count : bufferMap.values()) inBuffer = inBuffer + count;
	 
		int countDiff = initialVehicleCount - this.lostVehicles - parking - waiting - inQueue - inBuffer;
		
		if (countDiff != 0) 
		{
			log.error(e.getSimulationTime() + " Wrong number of vehicles in the Simulation - probably missed some Events! Difference: " + countDiff);
		}
	 }
	 

	/*
	 * Remove Link from Map, if VehicleCount has not changed or has changed more
	 * often, so that in the end its the same as in the beginning.
	 * 
	 * [TODO] Check the amount of time that is needed for this Check. Maybe it
	 * would be faster to recalculate the TravelTimes for all Links in the Map
	 * without check.
	 */
	private void filterChangedLinks() 
	{
		for (Iterator<Id> iterator = countChangedMap.keySet().iterator(); iterator.hasNext();)
		{
			Id id = iterator.next();
			// Same Count as in the last SimStep? -> remove it from "has changed" List
			if (countChangedMap.get(id) == countLastTimeStepMap.get(id)) iterator.remove();
		}
		countChangedInTimeStepMap.clear();
		countChangedInTimeStepMap.putAll(countChangedMap);

		countLastTimeStepMap.putAll(countChangedMap);
		
		countChangedMap.clear();
	}

	/*
	 * We assume that the Simulation uses MyLinkImpl instead of LinkImpl, so
	 * we don't check this for every Link...
	 */
	private synchronized void updateLinkVehicleCounts()
	{
		Map<Id, Integer> links2Update = getChangedLinkVehiclesCounts();
		
		/*
		 * We also could iterate over all NetworkLinks and check whether their
		 * VehiclesCount has changed - but in most cases only a few Links will change
		 * in a single SimStep so iterating over the changed Entry and look for
		 *  the corresponding Link should be faster...
		 *  [TODO] Check, whether this Assumption is true.
		 */
        for (Entry<Id, Integer> entry : links2Update.entrySet()) 
        {
            Id id = entry.getKey();
            Integer vehiclesCount = entry.getValue();
         
            // Assumption...
            MyLinkImpl link = (MyLinkImpl)((NetworkLayer) this.qNetwork.getNetwork()).getLinks().get(id);
            
            link.setVehiclesCount(vehiclesCount);
        }
        
//        for (Link link : this.queueNetwork.getNetworkLayer().getLinks().values())
//        {
//        	if (this.getLinkDrivingVehiclesCount(link.getId()) != ((MyLinkImpl)link).getVehiclesCount())
//        	{
//        		double v1 = this.getLinkDrivingVehiclesCount(link.getId());
//        		double v2 = ((MyLinkImpl)link).getVehiclesCount();
//        		log.error("Vehicles Count does not match! " + link.getId().toString() + " " + v1 + " " + v2);
//        	}
//        }
	}
	
	/*
	 * Returns a Map<LinkId, driving Vehicles on the Link>. The Map contains
	 * only those Links, where the number of driving Vehicles has changed within 
	 * in the current TimeStep of the QueueSimulation. If all Links are needed, 
	 * create Method like getLinkVehicleCounts().
	 */
	public Map<Id, Integer> getChangedLinkVehiclesCounts()
	{
		return countChangedInTimeStepMap;
	}

	/*
	 * Returns the number of Vehicles, that are driving on the Link.
	 */
	public int getLinkDrivingVehiclesCount(Id id) {
		int count = 0;

		count = count + waitingMap.get(id);
		count = count + vehQueueMap.get(id);
		count = count + bufferMap.get(id);

		return count;
	}

	public synchronized void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
//		log.info("SimStep done..." + e.getSimulationTime());
//		System.out.println("LinkVehiclesCounter QueueSimulationAfterSimStepEvent " + e.getSimulationTime() + "-------------------------------------------------------------------------------");
		// Check the vehicle count every Hour
		if (((int)e.getSimulationTime()) % 3600 == 0) checkVehicleCount(e);
		
		filterChangedLinks();
		updateLinkVehicleCounts();
	}

	public void notifySimulationInitialized(SimulationInitializedEvent e)
	{	
		this.agentCounter = ((QSim)e.getQueueSimulation()).getAgentCounter();
//		System.out.println("LinkVehiclesCounter QueueSimulationInitializedEvent-------------------------------------------------------------------------------");
		createInitialCounts();
		filterChangedLinks();
		updateLinkVehicleCounts();
	}

	public void reset(int iteration)
	{
		createInitialCounts();
		updateLinkVehicleCounts();
	}
}
