package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

public final class PersonExperiencedLeg {
	private final Id<Person> agentId;
	private final Leg leg;

	public PersonExperiencedLeg(Id<Person> agentId, Leg leg) {
		this.agentId = agentId;
		this.leg = leg;
	}

	public Id<Person> getAgentId() {
		return agentId;
	}

	public Leg getLeg() {
		return leg;
	}
}
