package playground.christoph.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
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
import org.matsim.core.mobsim.queuesim.QueueLane;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationAfterSimStepEvent;
import org.matsim.core.mobsim.queuesim.events.QueueSimulationInitializedEvent;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationAfterSimStepListener;
import org.matsim.core.mobsim.queuesim.listener.QueueSimulationInitializedListener;

import playground.christoph.network.MyLinkImpl;

/*
 * Counts the number of Vehicles on the QueueLinks of a given QueueNetwork.
 * 
 * Additional a List of Links with changed VehicleCount per TimeStep is
 * created.
 */
public class LinkVehiclesCounter2 implements BasicLinkEnterEventHandler,
		BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler,
		BasicAgentDepartureEventHandler, BasicAgentWait2LinkEventHandler,
		BasicAgentStuckEventHandler, QueueSimulationAfterSimStepListener,
		QueueSimulationInitializedListener {

	private QueueNetwork queueNetwork;

	private static final Logger log = Logger.getLogger(LinkVehiclesCounter2.class);
	/*
	 *  List of Links where the number of driving Cars has changed in the 
	 *  current TimeStep (and maybe changed back again...)
	 *  Is used internally and changed during a SimStep
	 */	
	Set<Id> changedLinkSet; // LinkId
	
	/*
	 * List of Links where the Number of driving has changed in the last 
	 * TimeStep. It can for example be used by LookupTables between two SimSteps
	 * to check for which Links they have to recalculate the TravelTimes and
	 * TravelCosts.
	 */
	Map<Id, Integer> countChangedInTimeStepMap; // Counts from the just ended TimeStep
	
	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
	}

	private synchronized void createInitialCounts() {
		
		// initialize the Data Structures
		changedLinkSet = new HashSet<Id>();
		
		countChangedInTimeStepMap = new HashMap<Id, Integer>();

		// collect the Counts
		for (QueueLink queueLink : queueNetwork.getLinks().values()) 
		{
			Id id = queueLink.getLink().getId();
			
			changedLinkSet.add(id);
			
			countChangedInTimeStepMap.put(id, 0);
		}
	}

	public synchronized void handleEvent(BasicLinkEnterEvent event) {

		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(BasicLinkLeaveEvent event) {

		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(BasicAgentArrivalEvent event) {

		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(BasicAgentDepartureEvent event) {
		
		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(BasicAgentWait2LinkEvent event) {

		// nothing to do...
	}

	public synchronized void handleEvent(BasicAgentStuckEvent event) {

		// nothing to do...
	}
 
	/*
	 * We assume that the Simulation uses MyLinkImpl instead of LinkImpl, so
	 * we don't check this for every Link...
	 */
	private synchronized void updateLinkVehicleCounts()
	{
		for (Id id : changedLinkSet)
		{            
            QueueLink queueLink = this.queueNetwork.getLinks().get(id);
            
    		int vehiclesCount = 0;
    		for (QueueLane queueLane : queueLink.getQueueLanes())
    		{
    			vehiclesCount = vehiclesCount + queueLane.getAllVehicles().size();
    		}
    		
    		// Assumption...
    		MyLinkImpl link = (MyLinkImpl)queueLink.getLink();
            link.setVehiclesCount(vehiclesCount);
            
            countChangedInTimeStepMap.put(id, vehiclesCount);
		}
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

	public synchronized void notifySimulationAfterSimStep(QueueSimulationAfterSimStepEvent e) {
//		log.info("SimStep done..." + e.getSimulationTime());
//		System.out.println("LinkVehiclesCounter QueueSimulationAfterSimStepEvent " + e.getSimulationTime() + "-------------------------------------------------------------------------------");
		
		updateLinkVehicleCounts();
	}

	public void notifySimulationInitialized(QueueSimulationInitializedEvent e)
	{	
//		System.out.println("LinkVehiclesCounter QueueSimulationInitializedEvent-------------------------------------------------------------------------------");
		createInitialCounts();
		updateLinkVehicleCounts();
	}

	public void reset(int iteration)
	{
		createInitialCounts();
		updateLinkVehicleCounts();
	}
}
