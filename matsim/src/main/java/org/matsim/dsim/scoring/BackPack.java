package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;

public class BackPack {

	private final Id<Person> personId;
	private final Collection<Event> events;
	private final Plan plan;
	private final BackpackPlan backpackPlan;

	// this can probably be done from the backpackplan
	private Id<Vehicle> currentVehicle;

	public Id<Person> personId() {
		return personId;
	}

	public Id<Vehicle> currentVehicle() {
		return currentVehicle;
	}

	public void setCurrentVehicle(Id<Vehicle> currentVehicle) {
		this.currentVehicle = currentVehicle;
	}

	public boolean isInVehicle() {
		return currentVehicle != null;
	}

	public BackpackPlan backpackPlan() {
		return backpackPlan;
	}

	public BackPack(Id<Person> personId, Network network) {
		this.personId = personId;
		this.events = new ArrayList<>();
		this.plan = PopulationUtils.createPlan();
		backpackPlan = new BackpackPlan(network);
	}

	void addSpecialScoringEvent(Event e) {
		if (isRelevantForScoring(e)) {
			events.add(e);
		} else {
			throw new IllegalArgumentException("Agents only take events relevant for the scoring function into their back back. Currently these are: " +
				"PersonMoneyEvent, PersonScoreEvent, PersonStuckEvent.");
		}
	}

	public static boolean isRelevantForScoring(Event e) {
		return e instanceof PersonMoneyEvent || e instanceof PersonScoreEvent || e instanceof PersonStuckEvent;
	}

}
