/**
 *
 */
package playground.johannes.eut;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.utils.misc.Time;


/**
 * An EventBasedTTProvider determines reactive travel times based on events.
 * Travel times are calculated as the difference between a link enter and link
 * leave event of the same vehicle. The retrieved travel times are averaged over
 * a floating time bin.
 *
 * @author illenberger
 *
 */
public class EventBasedTTProvider implements TravelTimeI, EventHandlerLinkEnterI, EventHandlerLinkLeaveI,
		EventHandlerAgentArrivalI {

	// =====================================================================
	// private fields
	// =====================================================================

	private int binsize;

//	private boolean eventsAvailable;

	private HashMap<Key2d<String, String>, Double> enterEvents = new HashMap<Key2d<String, String>, Double>();

	/*
	 * TODO: Check if it makes sense to use a LinkedHashMap instead.
	 */
	private LinkedList<TTElement> ttimes = new LinkedList<TTElement>();

	private Map<BasicLinkI, Averager> averagedTTimes = new HashMap<BasicLinkI, Averager>();

//	private SimTimeI simTime;

	private int lastCall = -1;

	// =====================================================================
	// constructor
	// =====================================================================

	public EventBasedTTProvider(int binsize) {
		this.binsize = binsize;
//		this.simTime = simTime;
	}

	// =====================================================================
	// instance methods
	// =====================================================================

	public void handleEvent(EventLinkEnter event) {
		Key2d<String, String> key = new Key2d<String, String>(event.linkId,
				event.agentId);
		this.enterEvents.put(key, event.time);

	}

	public void handleEvent(EventLinkLeave event) {
		Key2d<String, String> key = new Key2d<String, String>(event.linkId,
				event.agentId);
		Double t1 = this.enterEvents.remove(key);

		if (t1 != null) {
			double deltaT = event.time - t1;
			if (deltaT >= 0) {
				this.ttimes.add(new TTElement(event.link, (int) event.time,
						(int) deltaT));
//				eventsAvailable = true;
			}
		}
	}

	public void handleEvent(EventAgentArrival event) {
		/*
		 * Arrival event does not count as travel time!
		 */
		Key2d<String, String> key = new Key2d<String, String>(event.linkId,
				event.agentId);
		this.enterEvents.remove(key);
	}

	public void reset(int iteration) {
		this.enterEvents.clear();
		this.ttimes.clear();
		this.lastCall = -1;
	}

	public TravelTimeI requestLinkCost() {
		/*
		 * Average the travel times only if there are new events available.
		 */
		if ((SimulationTimer.getTime() - this.lastCall) > 0) {
			this.lastCall = (int) SimulationTimer.getTime();
//			eventsAvailable = false;

			/*
			 * Remove all events that are older than the current time minus
			 * the binsize.
			 */
			int lowerbound = (int) (SimulationTimer.getTime() - this.binsize);
			for (ListIterator<TTElement> it = this.ttimes.listIterator(); it
					.hasNext();) {
				TTElement e = it.next();
				if (e.getTimeStamp() < lowerbound)
					it.remove();
				else
					/*
					 * Since the events are in chronological order, we can
					 * be sure that all succeeding events are newer.
					 */
					break;
			}
			/*
			 * Average the remaining travel time elements...
			 */
			this.averagedTTimes = new HashMap<BasicLinkI, Averager>();
			for (TTElement e : this.ttimes) {
				Averager a = this.averagedTTimes.get(e.getLink());
				if (a == null) {
					a = new Averager();
					this.averagedTTimes.put(e.getLink(), a);
				}
				a.add(e.getTtime());
			}
		}
		/*
		 * This class itself implements the RoutableLinkCostI.
		 */
		return this;
	}

	public int getLinkTravelTime_s(BasicLinkI link, int time_s) {
		Averager a = this.averagedTTimes.get(link);
		if (a == null) {
			/*
			 * TODO: Replace this with a free travel time provider!
			 */
			return (int) link.getFreespeed(Time.UNDEFINED_TIME);
		} else {
			return (int) a.getAvg();
		}
	}

	public double getLinkTravelCost(BasicLinkI link, int time_s) {
		return this.getLinkTravelTime_s(link, time_s);
	}

//	public EvaluatedLinkCostI requestEvaluatedLinkCost() {
//		return null;
//	}
//
//	public TurningMoveCostI requestTurningMoveCost() {
//		return null;
//	}

	public IdI getId() {
		return null;
	}

	private class TTElement {

		private final BasicLinkI link;

		private final int timeStamp;

		private final int ttime;

		public TTElement(BasicLinkI link, int timeStamp, int ttime) {
			this.link = link;
			this.timeStamp = timeStamp;
			this.ttime = ttime;
		}

		public BasicLinkI getLink() {
			return this.link;
		}

		public int getTimeStamp() {
			return this.timeStamp;
		}

		public int getTtime() {
			return this.ttime;
		}
	}

	public double getLinkTravelTime(Link link, double time) {
		// TODO Auto-generated method stub
		return getLinkTravelTime_s(link, (int) time);
	}
}
