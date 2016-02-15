package gunnar.ihop2.transmodeler.run;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class IntervalSortingEventsManager implements EventsManager {

	// -------------------- MEMBERS --------------------

	private final EventsManager nextConsumer;

	private final double timeWindow_s;

	private final TreeSet<Event> mostRecentEvents;

	// -------------------- CONSTRUCTION --------------------

	IntervalSortingEventsManager(final EventsManager nextConsumer,
			final double timeWindow_s) {
		this.nextConsumer = nextConsumer;
		this.timeWindow_s = timeWindow_s;
		this.mostRecentEvents = new TreeSet<Event>(new EventComparator());
	}

	// -------------------- SORTING EVENT PROCESSING --------------------

	// final Set<String> eventTypes = new LinkedHashSet<>();
	final Set<String> eventStrings = new LinkedHashSet<String>();
	
	@Override
	public synchronized void processEvent(final Event event) {

		final String newString = event.toString();
		if (eventStrings.contains(newString)) {
			System.err.println("ALREADY THERE: " + newString);
		}
		
		// System.out.println(event.toString());
		// this.eventTypes.add(event.getEventType());

		if (!this.mostRecentEvents.add(event)) {
//			throw new RuntimeException("failed to add event " + event);
		}

		while (!this.mostRecentEvents.isEmpty()
				&& ((this.mostRecentEvents.last().getTime() - this.mostRecentEvents
						.first().getTime()) >= this.timeWindow_s)) {
			this.nextConsumer.processEvent(this.mostRecentEvents.pollFirst());
		}
	}

	// --------------- BELOW JUST PASS-THROUGH WRAPPERS ---------------

	@Override
	public void addHandler(final EventHandler handler) {
		this.nextConsumer.addHandler(handler);
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		this.nextConsumer.removeHandler(handler);
	}

	@Override
	public void resetHandlers(int iteration) {
		this.nextConsumer.resetHandlers(iteration);
	}

	@Override
	public void initProcessing() {
		this.nextConsumer.initProcessing();
	}

	@Override
	public void afterSimStep(double time) {
		this.nextConsumer.afterSimStep(time);
	}

	@Override
	public void finishProcessing() {
		this.nextConsumer.finishProcessing();
	}
}
