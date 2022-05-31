package org.matsim.codeexamples.scoring.pseudoRandomErrors.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public interface EpsilonProvider {
	double getEpsilon(Id<Person> personId, int tripIndex, Object alternative);
}
