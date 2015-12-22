package gunnar.ihop2.transmodeler.run;

import java.util.TreeSet;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class EventsSorter implements ActivityEndEventHandler,
		PersonDepartureEventHandler, PersonEntersVehicleEventHandler,
		VehicleEntersTrafficEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler,
		VehicleLeavesTrafficEventHandler, PersonLeavesVehicleEventHandler,
		PersonArrivalEventHandler, ActivityStartEventHandler {

	// -------------------- MEMBERS --------------------

	private final String fromFileName;

	private final String toFileName;

	private final double timeWindow_s;

	private final TreeSet<Event> mostRecentEvents;

	private EventWriterXML writer = null;
	
	// -------------------- CONSTRUCTION --------------------

	EventsSorter(final String fromFileName, final String toFileName,
			final double timeWindow_s) {
		this.fromFileName = fromFileName;
		this.toFileName = toFileName;
		this.timeWindow_s = timeWindow_s;
		this.mostRecentEvents = new TreeSet<Event>(new EventComparator());
	}

	
	void run() {
		final EventsManager eventsManager = EventsUtils.createEventsManager();
		// final EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(this);
		final MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(this.fromFileName);
	}

	private synchronized void processEvent(Event event) {
		
		if (!this.mostRecentEvents.add(event)) {
			throw new RuntimeException("failed to add event " + event);
		}

		while (!this.mostRecentEvents.isEmpty()
				&& ((this.mostRecentEvents.last().getTime() - this.mostRecentEvents
						.first().getTime()) >= this.timeWindow_s)) {
			final Event nextEvent = this.mostRecentEvents.pollFirst();
//			System.out.println(nextEvent);
		}
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.processEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.processEvent(event);
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		final EventsSorter es = new EventsSorter(
//				"./ihop2/transmodeler-matsim/exchange/events.xml", null, 2.0);
				"./ihop2/matsim-output/ITERS/it.0/0.events.xml.gz", null, 2.0);
		es.run();
		System.out.println("... DONE");
	}

}
