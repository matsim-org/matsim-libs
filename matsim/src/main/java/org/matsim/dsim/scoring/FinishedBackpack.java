package org.matsim.dsim.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.Collection;

public record FinishedBackpack(Id<Person> personId, int startingPartition, Collection<Event> events, Plan experiencedPlan) {
}
