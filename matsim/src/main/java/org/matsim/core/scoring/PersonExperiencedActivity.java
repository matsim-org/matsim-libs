package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

public final class PersonExperiencedActivity {
	private final Id<Person> agentId;
	private final Activity activity;

	public PersonExperiencedActivity(Id<Person> agentId, Activity activity) {
		this.agentId = agentId;
		this.activity = activity;
	}

	public Id<Person> getAgentId() {
		return agentId;
	}

	public Activity getActivity() {
		return activity;
	}
}
