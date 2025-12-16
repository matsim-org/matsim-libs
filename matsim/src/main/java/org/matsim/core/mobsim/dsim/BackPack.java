package org.matsim.core.mobsim.dsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public record BackPack(Id<Person> personId, Plan experiencedPlan) {
}
