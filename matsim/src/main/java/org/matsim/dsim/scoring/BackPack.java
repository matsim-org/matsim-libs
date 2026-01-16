package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Agents carry backpacks inside which they gather data of what they have experienced during the simulation.
 */
public class BackPack {

	private final Id<Person> personId;
	private final Collection<Event> events;
	private final BackpackPlan backpackPlan;
	private final int startingPartition;

	Id<Person> personId() {
		return personId;
	}

	Id<Vehicle> currentVehicle() {
		return backpackPlan.currentVehicle();
	}

	boolean isInVehicle() {
		return backpackPlan.isInVehicle();
	}

	BackpackPlan backpackPlan() {
		return backpackPlan;
	}

	public BackPack(Id<Person> personId, int startingPartition) {
		this.personId = personId;
		this.startingPartition = startingPartition;
		this.events = new ArrayList<>();
		backpackPlan = new BackpackPlan();
	}

	void addSpecialScoringEvent(Event e) {
		if (isRelevantForScoring(e)) {
			events.add(e);
		} else {
			throw new IllegalArgumentException("Agents only take events relevant for the scoring function into their back back. Currently these are: " +
				"PersonMoneyEvent, PersonScoreEvent, PersonStuckEvent.");
		}
	}

	Collection<Event> specialScoringEvents() {
		return events;
	}

	static boolean isRelevantForScoring(Event e) {
		return e instanceof PersonMoneyEvent || e instanceof PersonScoreEvent || e instanceof PersonStuckEvent;
	}

	FinishedBackpack finish(Network network, TransitSchedule transitSchedule) {
		backpackPlan.finish(network, transitSchedule);
		return new FinishedBackpack(personId, startingPartition, events, backpackPlan.experiencedPlan());
	}
}
