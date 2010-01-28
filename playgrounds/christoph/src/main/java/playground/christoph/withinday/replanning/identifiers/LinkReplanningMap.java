package playground.christoph.withinday.replanning.identifiers;

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
import org.matsim.ptproject.qsim.QLink;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QVehicle;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.Tuple;

/*
 * This Module is used by a LeaveLinkReplanner. It calculates the time
 * when an agent should do LeaveLinkReplanning.
 * 
 * The time is estimated as following:
 * When an LinkEnterEvent is thrown the Replanning Time is set to
 * the current time + the FreeSpeed Travel Time. This guarantees that
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
	
	private QNetwork qNetwork;

	// Repeated replanning if a person gets stuck in a Link
	private boolean repeatedReplanning = true;
	private double replanningInterval = 120.0;
	
	private static final Logger log = Logger.getLogger(LinkReplanningMap.class);

	private Map<Id, Tuple<Id, Double>> replanningMap;	// PersonId, Tuple<LinkId, ReplanningTime>
	
	public LinkReplanningMap(QSim qSim)
	{
		this.qNetwork = qSim.getQueueNetwork();
		
		//Add LinkReplanningMap to the QueueSimulation's EventsManager
		qSim.getEventsManager().addHandler(this);
		
		this.replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}
	
	// set the earliest possible leave link time as replanning time
	public void handleEvent(LinkEnterEvent event)
	{
		double now = event.getTime();
		QLink qLink = qNetwork.getQueueLink(event.getLinkId());
		double departureTime = (now + ((LinkImpl)qLink.getLink()).getFreespeedTravelTime(now));

		replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), departureTime));	
	}

	public void handleEvent(LinkLeaveEvent event)
	{
		replanningMap.remove(event.getPersonId());
	}

	public void handleEvent(AgentArrivalEvent event)
	{
		replanningMap.remove(event.getPersonId());
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
		replanningMap.put(event.getPersonId(), new Tuple<Id, Double>(event.getLinkId(), event.getTime()));
	}

	public void handleEvent(AgentStuckEvent event)
	{
		replanningMap.remove(event.getPersonId());
	}
	
	public Map<Id, Tuple<Id, Double>> getLinkReplanningMap()
	{
		return this.replanningMap;
	}
	
	public synchronized List<QVehicle> getReplanningVehicles(double time)
	{
		// using the ArrayList is just a Workaround...
		ArrayList<QVehicle> vehiclesToReplanLeaveLink = new ArrayList<QVehicle>();

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
				QVehicle vehicle = this.qNetwork.getQueueLink(linkId).getVehicle(personId);
				
				// Repeated Replanning per Link possible? 
				if (repeatedReplanning) entry.setValue(new Tuple<Id,Double>(linkId, time + this.replanningInterval));
				else entries.remove();
				
				vehiclesToReplanLeaveLink.add(vehicle);
			}
		}
		
		return vehiclesToReplanLeaveLink;
	}

	public void reset(int iteration)
	{
		replanningMap = new HashMap<Id, Tuple<Id, Double>>();
	}
}
