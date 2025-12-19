package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;

public class BackPack {

	private final Id<Person> personId;
	private final Collection<Event> events;
	private final BackpackPlan backpackPlan;

	public Id<Person> personId() {
		return personId;
	}

	public Id<Vehicle> currentVehicle() {
		return backpackPlan.currentVehicle();
	}

	public boolean isInVehicle() {
		return backpackPlan.currentVehicle() != null;
	}

	public BackpackPlan backpackPlan() {
		return backpackPlan;
	}

	public BackPack(Id<Person> personId) {
		this.personId = personId;
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

	public Collection<Event> specialScoringEvents() {
		return events;
	}

	public static boolean isRelevantForScoring(Event e) {
		return e instanceof PersonMoneyEvent || e instanceof PersonScoreEvent || e instanceof PersonStuckEvent;
	}
}
