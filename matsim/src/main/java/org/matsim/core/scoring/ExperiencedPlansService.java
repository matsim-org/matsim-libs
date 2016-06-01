package org.matsim.core.scoring;


import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import java.util.Map;

public interface ExperiencedPlansService {

	void writeExperiencedPlans(String iterationFilename);

	Map<Id<Person>, Plan> getAgentRecords();

}
