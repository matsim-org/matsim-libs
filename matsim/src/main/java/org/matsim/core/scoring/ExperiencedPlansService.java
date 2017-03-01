package org.matsim.core.scoring;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.Map;

public interface ExperiencedPlansService {

	void writeExperiencedPlans(String filename);

	Map<Id<Person>, Plan> getExperiencedPlans();

	void finishIteration();

}
