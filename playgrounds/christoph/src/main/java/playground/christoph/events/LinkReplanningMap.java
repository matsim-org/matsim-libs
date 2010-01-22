package playground.christoph.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;

/*
 * This Module is used by a LeaveLinkReplanner. It calculates the time
 * when an agent should do LeaveLinkReplanning.
 * 
 * The time is estimated as following:
 * When an LinkEnterEvent is thrown the Replanning Time is set to
 * the current time + the Freespeed Travel Time. This guarantees that
 * the replanning will be done while the agent is on the Link.
 * 
 * Additionally a Replanning Interval can be set. This allows an Agent
 * to do multiple Replanning on a single Link. This may be useful if the
 * Traffic System is congested and the Link Travel Times are much longer
 * than the Freespeed Travel Times. 
 */

public class LinkReplanningMap implements LinkEnterEventHandler,
		LinkLeaveEventHandler, AgentArrivalEventHandler,
		AgentDepartureEventHandler, AgentWait2LinkEventHandler,
		AgentStuckEventHandler {
	
	private QueueNetwork queueNetwork;

	// Repeated replanning if a person gets stuck in a Link
	private boolean repeatedReplanning = true;
	private double replanningInterval = 120.0;
	
	private static final Logger log = Logger.getLogger(LinkReplanningMap.class);

	private Map<Id, Tuple<Id, Double>> replanningMap;	// PersonId, Tuple<LinkId, ReplanningTime>
	
	public LinkReplanningMap()
	{
		this.replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}
	
	public void setQueueNetwork(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
	}
	
	// set the earliest possible leave link time as replanning time 
	public void handleEvent(LinkEnterEvent event)
	{
		double now = event.getTime();
		QueueLink queueLink = queueNetwork.getQueueLink(event.getLinkId());
		double departureTime = (now + ((LinkImpl)queueLink.getLink()).getFreespeedTravelTime(now));

		synchronized(replanningMap)
		{
			replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), departureTime));	
		}
	}

	public void handleEvent(LinkLeaveEvent event)
	{
		synchronized(replanningMap)
		{
			replanningMap.remove(event.getPersonId());
		}
	}

	public void handleEvent(AgentArrivalEvent event)
	{
		synchronized(replanningMap)
		{
			replanningMap.remove(event.getPersonId());
		}
	}

	// Nothing to do here...
	public void handleEvent(AgentDepartureEvent event)
	{

	}

	/*
	 * Person is added directly to the Buffer Queue so we don't need a
	 * time offset here.
	 */
	public void handleEvent(AgentWait2LinkEvent event)
	{		
		synchronized(replanningMap)
		{
			replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), event.getTime()));
		}
	}

	public void handleEvent(AgentStuckEvent event)
	{
		synchronized(replanningMap)
		{
			replanningMap.remove(event.getPersonId());
		}
	}
	
	public Map<Id, Tuple<Id, Double>> getLinkReplanningMap()
	{
		return this.replanningMap;
	}
	
	public synchronized List<QueueVehicle> getReplanningVehicles(double time)
	{
		// using the ArrayList is just a Workaround...
		ArrayList<QueueVehicle> vehiclesToReplanLeaveLink = new ArrayList<QueueVehicle>();

		Iterator<Entry<Id, Tuple<Id, Double>>> entries = replanningMap.entrySet().iterator();
		while (entries.hasNext())
		{
			Entry<Id, Tuple<Id, Double>> entry = entries.next();
			Id personId = entry.getKey();
			Id linkId = entry.getValue().getFirst();
          			
			double replanningTime = entry.getValue().getSecond();
	       
			if (time >= replanningTime)
			{
				// check whether the replanning flag is set - if not, skip the person
				QueueVehicle vehicle = this.queueNetwork.getQueueLink(linkId).getVehicle(personId);
//				boolean replanning = (Boolean)vehicle.getDriver().getPerson().getCustomAttributes().get("leaveLinkReplanning");
//				if(!replanning)
//				{
//					entries.remove();
//					continue;
//				}
				
				// Repeated Replanning per Link possible? 
				if (repeatedReplanning) entry.setValue(new Tuple<Id,Double>(linkId, time + this.replanningInterval));
				else entries.remove();
				
				vehiclesToReplanLeaveLink.add(vehicle);
			}
		}
		
		return vehiclesToReplanLeaveLink;
	}

	public synchronized void reset(int iteration)
	{
		replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}
}
