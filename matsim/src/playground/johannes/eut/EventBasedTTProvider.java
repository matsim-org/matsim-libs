/**
 *
 */
package playground.johannes.eut;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.network.BasicLink;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.LinkLeaveEventHandler;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.router.util.TravelTime;


/**
 * An EventBasedTTProvider determines reactive travel times based on events.
 * Travel times are calculated as the difference between a link enter and link
 * leave event of the same vehicle. The retrieved travel times are averaged over
 * a floating time bin.
 *
 * @author illenberger
 *
 */
public class EventBasedTTProvider implements TravelTime, LinkEnterEventHandler, LinkLeaveEventHandler,
		AgentArrivalEventHandler {

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

	private Map<BasicLink, Averager> averagedTTimes = new HashMap<BasicLink, Averager>();

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

	public void handleEvent(LinkEnterEventImpl event) {
		Key2d<String, String> key = new Key2d<String, String>(event.getLinkId().toString(),
				event.getPersonId().toString());
		this.enterEvents.put(key, event.getTime());

	}

	public void handleEvent(LinkLeaveEventImpl event) {
		Key2d<String, String> key = new Key2d<String, String>(event.getLinkId().toString(),
				event.getPersonId().toString());
		Double t1 = this.enterEvents.remove(key);

		if (t1 != null) {
			double deltaT = event.getTime() - t1;
			if (deltaT >= 0) {
				this.ttimes.add(new TTElement(event.getLink(), (int) event.getTime(),
						(int) deltaT));
//				eventsAvailable = true;
			}
		}
	}

	public void handleEvent(AgentArrivalEventImpl event) {
		/*
		 * Arrival event does not count as travel time!
		 */
		Key2d<String, String> key = new Key2d<String, String>(event.getLinkId().toString(),
				event.getPersonId().toString());
		this.enterEvents.remove(key);
	}

	public void reset(int iteration) {
		this.enterEvents.clear();
		this.ttimes.clear();
		this.lastCall = -1;
	}

	public TravelTime requestLinkCost() {
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
			this.averagedTTimes = new HashMap<BasicLink, Averager>();
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

	public int getLinkTravelTime_s(BasicLink link, int time_s) {
		Averager a = this.averagedTTimes.get(link);
		if (a == null) {
			/*
			 * TODO: Replace this with a free travel time provider!
			 */
			return (int) (link.getLength()/link.getFreespeed(time_s));
		} else {
			return (int) a.getAvg();
		}
	}

	public double getLinkTravelCost(BasicLink link, int time_s) {
		return this.getLinkTravelTime_s(link, time_s);
	}

//	public EvaluatedLinkCostI requestEvaluatedLinkCost() {
//		return null;
//	}
//
//	public TurningMoveCostI requestTurningMoveCost() {
//		return null;
//	}

	public Id getId() {
		return null;
	}

	private class TTElement {

		private final BasicLink link;

		private final int timeStamp;

		private final int ttime;

		public TTElement(BasicLink link, int timeStamp, int ttime) {
			this.link = link;
			this.timeStamp = timeStamp;
			this.ttime = ttime;
		}

		public BasicLink getLink() {
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
