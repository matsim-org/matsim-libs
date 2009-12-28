package playground.christoph.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
 * After each SimStep the number of Vehicles is written to the MyLinks of 
 * the Network. To do so it is necessary to use a MyLinkFactoryImpl in the
 * Controller! 
 * 
 * Additional a List of Links with changed VehicleCount per TimeStep is
 * created.
 */
public class LinkVehiclesCounter2 implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler,
		AgentDepartureEventHandler, AgentWait2LinkEventHandler,
		AgentStuckEventHandler, QueueSimulationAfterSimStepListener,
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

	public synchronized void handleEvent(LinkEnterEvent event) {

		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(LinkLeaveEvent event) {

		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(AgentArrivalEvent event) {

		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(AgentDepartureEvent event) {
		
		Id id = event.getLinkId();
		changedLinkSet.add(id);
	}

	public synchronized void handleEvent(AgentWait2LinkEvent event) {

		// nothing to do...
	}

	public synchronized void handleEvent(AgentStuckEvent event) {

		// nothing to do...
	}
 
	/*
	 * We assume that the Simulation uses MyLinkImpl instead of LinkImpl, so
	 * we don't check this for every Link...
	 */
	private synchronized void updateLinkVehicleCounts()
	{
		Iterator<Id> iter = changedLinkSet.iterator();
		while(iter.hasNext())
		{   
			Id id = iter.next();
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
            
            iter.remove();
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
