package playground.christoph.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentWait2LinkEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;

public class LinkVehiclesCounter implements BasicLinkEnterEventHandler,
		BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler,
		BasicAgentDepartureEventHandler, BasicAgentWait2LinkEventHandler,
		BasicAgentStuckEventHandler, QueueSimulationAfterSimStepListener,
		QueueSimulationInitializedListener {

	/*
	 * Counts the number of Vehicles on the QueueLinks of a given QueueNetwork.
	 * 
	 * Additional a List of Links with changed VehicleCount per TimeStep is
	 * created.
	 */


	private QueueNetwork queueNetwork;

	private static final Logger log = Logger.getLogger(LinkVehiclesCounter.class);

	// cyclical Status information - adapted from QueueSimulation
	protected static final int INFO_PERIOD = 3600;

	Map<Id, Integer> waitingMap;
	Map<Id, Integer> vehQueueMap;
	Map<Id, Integer> parkingMap;
	// Map<Id, Integer> transitVehicleStopQueueMap; // maybe later...
	Map<Id, Integer> bufferMap;

	Map<Id, Integer> countChangedMap; // LinkId, CarCount driving on the Link
	Map<Id, Integer> previousCountsMap; // Counts from the previous TimeStep

	int lostVehicles = 0;
	int initialVehicleCount;

	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
		
		// Doing this will create the Maps and fill them with "0" Entries.
		createInitialCounts();
	}
	
	private void createInitialCounts() {
		
		// initialize the Data Structures
		parkingMap = new HashMap<Id, Integer>();
		waitingMap = new HashMap<Id, Integer>();
		vehQueueMap = new HashMap<Id, Integer>();
		bufferMap = new HashMap<Id, Integer>();

		countChangedMap = new HashMap<Id, Integer>();
		previousCountsMap = new HashMap<Id, Integer>();

		// collect the Counts
		for (QueueLink queueLink : queueNetwork.getLinks().values()) {
			int vehCount = queueLink.getAllVehicles().size();

			initialVehicleCount = initialVehicleCount + vehCount;

			Id id = queueLink.getLink().getId();
			parkingMap.put(id, vehCount);
			waitingMap.put(id, 0);
			vehQueueMap.put(id, 0);
			bufferMap.put(id, 0);

			// initially every LinkCount has changed
			countChangedMap.put(id, 0);
		}
	}

	
	public void handleEvent(BasicLinkEnterEvent event) {
		// log.info("BasicLinkEnterEvent");

		Id id = event.getLinkId();

		int vehCount;
		vehCount = vehQueueMap.get(id);
		vehCount++;
		vehQueueMap.put(id, vehCount);

		countChangedMap.put(id, vehCount);
	}

	public void handleEvent(BasicLinkLeaveEvent event) {
		// log.info("BasicLinkLeaveEvent");
		
		Id id = event.getLinkId();

		int vehCount;
		vehCount = bufferMap.get(id);
		vehCount--;
		bufferMap.put(id, vehCount);

		countChangedMap.put(id, vehCount);
	}

	public void handleEvent(BasicAgentArrivalEvent event) {
		// log.info("BasicAgentArrivalEvent");

		Id id = event.getLinkId();

		int vehCount;
		vehCount = vehQueueMap.get(id);
		vehCount--;
		vehQueueMap.put(id, vehCount);

		vehCount = parkingMap.get(id);
		vehCount++;
		parkingMap.put(id, vehCount);

		countChangedMap.put(id, vehCount);
	}

	/*
	 * Structure of this method: Have a look at QueueSimulation.agentDeparts()...
	 */
	public void handleEvent(BasicAgentDepartureEvent event) {
		// log.info("BasicAgentDepartureEvent");

		//Handling depends on the Route of the Agent		
		LegImpl leg = ((AgentDepartureEvent)event).getLeg();
		LinkImpl link = queueNetwork.getNetworkLayer().getLink(event.getLinkId());
		
		if(leg.getMode().equals(TransportMode.car))
		{
			Id id = event.getLinkId();
			
			int vehCount;
			vehCount = parkingMap.get(id);
			vehCount--;
			parkingMap.put(id, vehCount);
			
			NetworkRoute route = (NetworkRoute) leg.getRoute();
			if (route.getEndLink() == link) 
			{
				// nothing to do here...
			} 
			else 
			{	
				vehCount = waitingMap.get(id);
				vehCount++;
				waitingMap.put(id, vehCount);
	
				countChangedMap.put(id, vehCount);
			}
		}
		else
		{
			log.error("Unknown Leg Mode!");
		}
	}


	public void handleEvent(BasicAgentWait2LinkEvent event) {
		// log.info("BasicAgentWait2LinkEvent");

		Id id = event.getLinkId();

		int vehCount;
		vehCount = waitingMap.get(id);
		vehCount--;
		waitingMap.put(id, vehCount);

		vehCount = bufferMap.get(id);
		vehCount++;
		bufferMap.put(id, vehCount);
	}

	
	public void handleEvent(BasicAgentStuckEvent event) {
		// log.info("BasicAgentStuckEvent");

		lostVehicles++;

		event.getLinkId();

		// where to remove it?
	}

	/*
	 * Looks like that there is a small Error in the counting of the VehQueue Map
	 * (and only in this Map what is quite strange because i have no idea how this is possible)
	 * 
	 * To use this method, some getters have to be added to the  QueueLink Class!
	 */
/*
	private void checkVehicleCount(QueueSimulationAfterSimStepEvent e) {
		int parking = 0;
		int waiting = 0;
		int inQueue = 0;
		int inBuffer = 0;

		for (int count : parkingMap.values())
			parking = parking + count;
		for (int count : waitingMap.values())
			waiting = waiting + count;
		for (int count : vehQueueMap.values())
			inQueue = inQueue + count;
		for (int count : bufferMap.values())
			inBuffer = inBuffer + count;

		int countDiff = initialVehicleCount - this.lostVehicles - parking - waiting - inQueue - inBuffer;
		if (countDiff != 0) {
			Log.error(e.getSimulationTime() + " Wrong number of vehicles in the Simulation - probably missed some Events! Difference: "	+ countDiff);
		}

		if (e.getSimulationTime() >= infoTime) {
			infoTime += INFO_PERIOD;
			log.info("SIMULATION AT " + Time.writeTime(e.getSimulationTime()) + " checking parking Vehicles Counts");			 
		}

		for (QueueLink queueLink : queueNetwork.getLinks().values()) 
		{
			Id id = queueLink.getLink().getId();
			
			int inBufferCount = queueLink.getVehiclesInBuffer().size();
			int myInBufferCount = bufferMap.get(id);
			if (inBufferCount != myInBufferCount)
			{
				int diff = inBufferCount - myInBufferCount;
				bufferMap.put(id, inBufferCount);
				
				int myVehQueueCount = vehQueueMap.get(queueLink.getLink().getId());
				vehQueueMap.put(id, myVehQueueCount - diff);
			}
			
			if (Simulation.getLost() != lostVehicles) 
			{
				log.error("Wrong LostCount");
				log.error("Expected: " + Simulation.getLost() + ", Found: " + lostVehicles);
			}
			if (queueLink.parkingCount() != parkingMap.get(id)) 
			{
				log.error("Wrong ParkingCount on Link " + id);
				log.error("Expected: " + queueLink.parkingCount() + ", Found: " + parkingMap.get(id));
			}
			if (queueLink.bufferCount() != bufferMap.get(id)) 
			{
				log.error("Wrong BufferCount on Link " + id);
				log.error("Expected: " + queueLink.bufferCount() + ", Found: " + bufferMap.get(id));
			}
			if (queueLink.vehQueueCount() != vehQueueMap.get(id)) 
			{
				log.error("Wrong VehicleQueueCount on Link " + id);
				log.error("Expected: " + queueLink.vehQueueCount() + ", Found: " + vehQueueMap.get(id));
			}
			if (queueLink.waitingCount() != waitingMap.get(id)) 
			{
				log.error("Wrong WaitingCount on Link " + id);
				log.error("Expected: " + queueLink.waitingCount() + ", Found: " + waitingMap.get(id));
			}
			
		}
	}
*/
	
	/*
	 * Remove Link from Map, if VehicleCount has not changed or has changed more
	 * often, so that in the end its the same as in the beginning.
	 * 
	 * [TODO] Check the amount of time that is needed for this Check. Maybe it
	 * would be faster to recalculate the TravelTimes for all Links in the Map
	 * without check.
	 */
	private void filterChangedLinks() {
		for (Iterator<Id> iterator = countChangedMap.keySet().iterator(); iterator
				.hasNext();) {
			Id id = iterator.next();
			if (countChangedMap.get(id) == previousCountsMap.get(id))
				iterator.remove();
		}
		previousCountsMap.clear();
		previousCountsMap.putAll(countChangedMap);

		countChangedMap.clear();
	}

	/*
	 * Returns a Map<LinkId, driving Vehicles on the Link>. The Map contains
	 * only those Links, where the number of Vehicles has changed within in the
	 * current TimeStep of the QueueSimulation. If all Links are needed, use
	 * getLinkVehicleCounts().
	 */
	public Map<Id, Integer> getChangedLinkVehiclesCounts() {
		return previousCountsMap;
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

	
	public void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e) {
//		checkVehicleCount(e);
		filterChangedLinks();
	}

	
	public void notifySimulationInitialized(QueueSimulationInitializedEvent e) {
		createInitialCounts();
	}

	
	public void reset(int iteration) {
		createInitialCounts();
	}
}
