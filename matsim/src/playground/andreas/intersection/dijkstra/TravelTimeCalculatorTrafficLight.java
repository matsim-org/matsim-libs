package playground.andreas.intersection.dijkstra;

import java.util.HashMap;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.trafficmonitoring.TravelTimeCalculator;

public class TravelTimeCalculatorTrafficLight extends TravelTimeCalculator {

	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<String, EnterEvent> enterEvents = new HashMap<String, EnterEvent>();
	private NetworkLayer network = null;
	final int roleIndex;
	private final int timeslice;
	private final int numSlots;


	public TravelTimeCalculatorTrafficLight(final NetworkLayer network) {
		this(network, 15*60, 30*3600);	// default timeslot-duration: 15 minutes
	}

	public TravelTimeCalculatorTrafficLight(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
	}

	public TravelTimeCalculatorTrafficLight(final NetworkLayer network, final int timeslice, final int maxTime) {
		super(network, timeslice);
		this.network = network;
		this.timeslice = timeslice;
		this.numSlots = (maxTime / this.timeslice) + 1;
		this.roleIndex = network.requestLinkRole();
		resetTravelTimes();
	}

	@Override
	public void resetTravelTimes() {
		for (Link inLink : this.network.getLinks().values()) {
			for (Link outLink : inLink.getToNode().getOutLinks().values()) {
				TravelTimeRole r = getTravelTimeRole(inLink, outLink);
				r.resetTravelTimes();
			}			
		}
		this.enterEvents.clear();
	}

	@Override
	public void reset(final int iteration) {
		/* DO NOT CALL resetTravelTimes here!
		 * reset(iteration) is called at the beginning of an iteration, but we still
		 * need the travel times from the last iteration for the replanning!
		 * That's why there is a separat method resetTravelTimes() which can
		 * be called after the replanning.      -marcel/20jan2008
		 */
	}

	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	@Override
	public void handleEvent(final EventLinkEnter event) {

		if (event.link == null) {
			event.link = (Link)this.network.getLocation(event.linkId);
		}
		
		EnterEvent newEvent = new EnterEvent(event.link, event.time);
		
		if(this.enterEvents.containsKey(event.agentId)){
			
			EnterEvent oldEvent = this.enterEvents.remove(event.agentId);
			double timediff = newEvent.time - oldEvent.time;
			getTravelTimeRole(oldEvent.link, newEvent.link).addTravelTime(newEvent.time, timediff);
						
		}				
		this.enterEvents.put(event.agentId, newEvent);
	}

	@Override
	public void handleEvent(final EventLinkLeave event) {
//		EnterEvent e = this.enterEvents.remove(event.agentId);
//		if ((e != null) && e.linkId.equals(event.linkId)) {
//			double timediff = event.time - e.time;
//			if (event.link == null) event.link = (Link)this.network.getLocation(event.linkId);
//			if (event.link != null) {
//				getTravelTimeRole(event.link).addTravelTime(e.time, timediff);
//			}
//		}
	}

	@Override
	public void handleEvent(final EventAgentArrival event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.enterEvents.remove(event.agentId);
	}

	private TravelTimeRole getTravelTimeRole(final Link fromLink, final Link toLink) {
		
		HashMap<Link, TravelTimeRole> travelTimeMap = (HashMap<Link, TravelTimeRole>) fromLink.getRole(this.roleIndex);
		if(travelTimeMap == null){
			travelTimeMap = new HashMap<Link, TravelTimeRole>();
			fromLink.setRole(this.roleIndex, travelTimeMap);			
		}
		
		TravelTimeRole r = travelTimeMap.get(toLink);		
		if (r == null) {
			r = new TravelTimeRole(fromLink, this.numSlots);
			travelTimeMap.put(toLink, r);
		}
		return r;
	}

	/*default*/ int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.timeslice;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}


	//////////////////////////////////////////////////////////////////////
	// Implementation of TravelTimeI
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.network.TravelCostI#getLinkTravelTime(org.matsim.network.Link, int)
	 */
	@Override
	public double getLinkTravelTime(final Link link, final double time) {
		return getTravelTimeRole(this.network.getLink(link.getFromNode().getId().toString()), this.network.getLink(link.getToNode().getId().toString())).getTravelTime(time);
	}


	static private class EnterEvent {

		public final Link link;
		public final double time;

		public EnterEvent(final Link link, final double time) {
			this.link = link;
			this.time = time;
		}

	};

	private class TravelTimeRole {
		private final double[] timeSum;
		private final int[] timeCnt;
		private final double[] travelTimes;
		private final Link fromLink;

		public TravelTimeRole(final Link fromLink, final int numSlots) {
			this.timeSum = new double[numSlots];
			this.timeCnt = new int[numSlots];
			this.travelTimes = new double[numSlots];
			this.fromLink = fromLink;
			resetTravelTimes();
		}

		public void resetTravelTimes() {
			for (int i = 0; i < this.timeSum.length; i++) {
				this.timeSum[i] = 0.0;
				this.timeCnt[i] = 0;
				this.travelTimes[i] = -1.0;
			}
		}

		public void addTravelTime(final double now, final double traveltime) {
			int index = getTimeSlotIndex(now);
			double sum = this.timeSum[index];
			int cnt = this.timeCnt[index];
			sum += traveltime;
			cnt++;
			this.timeSum[index] = sum;
			this.timeCnt[index] = cnt;
			this.travelTimes[index] = -1.0; // initialize with negative value
		}

		public double getTravelTime(final double now) {
			int index = getTimeSlotIndex(now);
			double ttime = this.travelTimes[index];
			if (ttime >= 0.0) return ttime; // negative values are invalid.

			int cnt = this.timeCnt[index];
			if (cnt == 0) {
				this.travelTimes[index] = this.fromLink.getLength() / this.fromLink.getFreespeed(now);
				return this.travelTimes[index];
			}

			double sum = this.timeSum[index];
			this.travelTimes[index] = sum / cnt;
			return this.travelTimes[index];
		}

	};

}
