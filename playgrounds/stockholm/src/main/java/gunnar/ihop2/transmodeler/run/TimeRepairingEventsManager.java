package gunnar.ihop2.transmodeler.run;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import floetteroed.utilities.math.BasicStatistics;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TimeRepairingEventsManager implements EventsManager {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final EventsManager nextConsumer;

	private Double prevTime = Double.NEGATIVE_INFINITY;

	final Set<Id<Person>> unknownPersonsIDs = new LinkedHashSet<>();
	final Set<Id<Link>> unknownLinkIDs = new LinkedHashSet<>();

	final BasicStatistics timeCorrectionStats = new BasicStatistics();

	// -------------------- CONSTRUCTION --------------------

	TimeRepairingEventsManager(final Scenario scenario,
			final EventsManager nextConsumer) {
		this.scenario = scenario;
		this.nextConsumer = nextConsumer;
	}

	// -------------------- SORTING EVENT PROCESSING --------------------

	private boolean isUnknownPerson(final Id<Person> personId) {
		if (!this.scenario.getPopulation().getPersons().containsKey(personId)) {
			this.unknownPersonsIDs.add(personId);
			return true;
		} else {
			return false;
		}
	}

	private boolean isUnknownLink(final Id<Link> linkId) {
		if (!scenario.getNetwork().getLinks().containsKey(linkId)) {
			this.unknownLinkIDs.add(linkId);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized void processEvent(Event event) {

		this.timeCorrectionStats.add(Math.max(this.prevTime - event.getTime(),
				0.0));

		// TODO This is messy code.

		if (event instanceof ActivityEndEvent) {
			final ActivityEndEvent originalEvent = (ActivityEndEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new ActivityEndEvent(this.prevTime,
						originalEvent.getPersonId(), originalEvent.getLinkId(),
						originalEvent.getFacilityId(),
						originalEvent.getActType());
			}

		} else if (event instanceof PersonDepartureEvent) {
			final PersonDepartureEvent originalEvent = (PersonDepartureEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new PersonDepartureEvent(this.prevTime,
						originalEvent.getPersonId(), originalEvent.getLinkId(),
						originalEvent.getLegMode());
			}

		} else if (event instanceof PersonEntersVehicleEvent) {
			final PersonEntersVehicleEvent originalEvent = (PersonEntersVehicleEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new PersonEntersVehicleEvent(this.prevTime,
						originalEvent.getPersonId(),
						originalEvent.getVehicleId());
			}

		} else if (event instanceof VehicleEntersTrafficEvent) {
			final VehicleEntersTrafficEvent originalEvent = (VehicleEntersTrafficEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new VehicleEntersTrafficEvent(this.prevTime,
						originalEvent.getPersonId(), originalEvent.getLinkId(),
						originalEvent.getVehicleId(),
						originalEvent.getNetworkMode(),
						originalEvent.getRelativePositionOnLink());
			}

		} else if (event instanceof LinkLeaveEvent) {
			final LinkLeaveEvent originalEvent = (LinkLeaveEvent) event;
			if (this.isUnknownPerson(originalEvent.getDriverId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new LinkLeaveEvent(this.prevTime,
						originalEvent.getVehicleId(), originalEvent.getLinkId());
			}

		} else if (event instanceof LinkEnterEvent) {
			final LinkEnterEvent originalEvent = (LinkEnterEvent) event;
			if (this.isUnknownPerson(originalEvent.getDriverId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new LinkEnterEvent(this.prevTime,
						originalEvent.getVehicleId(), originalEvent.getLinkId());
			}

		} else if (event instanceof VehicleLeavesTrafficEvent) {
			final VehicleLeavesTrafficEvent originalEvent = (VehicleLeavesTrafficEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new VehicleLeavesTrafficEvent(this.prevTime,
						originalEvent.getPersonId(), originalEvent.getLinkId(),
						originalEvent.getVehicleId(),
						originalEvent.getNetworkMode(),
						// TODO As inefficient as it can get:
						Double.parseDouble(originalEvent.getAttributes().get(
								VehicleLeavesTrafficEvent.ATTRIBUTE_POSITION)));
			}

		} else if (event instanceof PersonLeavesVehicleEvent) {
			final PersonLeavesVehicleEvent originalEvent = (PersonLeavesVehicleEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new PersonLeavesVehicleEvent(this.prevTime,
						originalEvent.getPersonId(),
						originalEvent.getVehicleId());
			}

		} else if (event instanceof PersonArrivalEvent) {
			final PersonArrivalEvent originalEvent = (PersonArrivalEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new PersonArrivalEvent(this.prevTime,
						originalEvent.getPersonId(), originalEvent.getLinkId(),
						originalEvent.getLegMode());
			}

		} else if (event instanceof ActivityStartEvent) {
			final ActivityStartEvent originalEvent = (ActivityStartEvent) event;
			if (this.isUnknownPerson(originalEvent.getPersonId())) {
				return;
			}
			if (this.isUnknownLink(originalEvent.getLinkId())) {
				Logger.getLogger(this.getClass().getName()).warning(
						"Event with unknown link: " + event);
			}
			if (this.prevTime == null || this.prevTime <= event.getTime()) {
				this.prevTime = event.getTime();
			} else {
				event = new ActivityStartEvent(this.prevTime,
						originalEvent.getPersonId(), originalEvent.getLinkId(),
						originalEvent.getFacilityId(),
						originalEvent.getActType());
			}

		} else {
			Logger.getLogger(this.getClass().getName()).warning(
					"Unknown event type: " + event);
		}

		// finally ...
		this.nextConsumer.processEvent(event);

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
