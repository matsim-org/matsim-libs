package playground.christoph.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.utils.collections.Tuple;

public class LinkReplanningMap implements BasicLinkEnterEventHandler,
		BasicLinkLeaveEventHandler, BasicAgentArrivalEventHandler,
		BasicAgentDepartureEventHandler, BasicAgentWait2LinkEventHandler,
		BasicAgentStuckEventHandler {

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
	public void handleEvent(BasicLinkEnterEvent event)
	{
		double now = event.getTime();
		QueueLink queueLink = queueNetwork.getQueueLink(event.getLinkId());
		double departureTime = (now + queueLink.getLink().getFreespeedTravelTime(now));
		
		replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), departureTime));
	}

	public void handleEvent(BasicLinkLeaveEvent event)
	{
		replanningMap.remove(event.getPersonId());
	}

	public void handleEvent(BasicAgentArrivalEvent event)
	{
		replanningMap.remove(event.getPersonId());
	}

	// Nothing to do here...
	public void handleEvent(BasicAgentDepartureEvent event)
	{

	}

	/*
	 * Person is added directly to the Buffer Queue so we don't need a
	 * time offset here.
	 */
	public void handleEvent(BasicAgentWait2LinkEvent event)
	{
		replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), event.getTime()));
	}

	public void handleEvent(BasicAgentStuckEvent event)
	{
		replanningMap.remove(event.getPersonId());
	}
	
	public Map<Id, Tuple<Id, Double>> getLinkReplanningMap()
	{
		return this.replanningMap;
	}
	
	public List<QueueVehicle> getReplanningVehicles(double time)
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
				// Repeated Replanning per Link possible? 
				if (repeatedReplanning) entry.setValue(new Tuple<Id,Double>(linkId, time + this.replanningInterval));
				else entries.remove();
				
				//personsToReplanMap.put(personId, linkId);
				vehiclesToReplanLeaveLink.add(this.queueNetwork.getQueueLink(linkId).getVehicle(personId));
			}
		}
		
		return vehiclesToReplanLeaveLink;
	}

	public void reset(int iteration)
	{
		replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}
}
