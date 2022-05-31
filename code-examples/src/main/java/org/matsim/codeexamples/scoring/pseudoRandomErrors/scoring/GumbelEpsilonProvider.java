package org.matsim.codeexamples.scoring.pseudoRandomErrors.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class GumbelEpsilonProvider extends AbstractEpsilonProvider {
	private final double scale;

	public GumbelEpsilonProvider(long randomSeed, double scale) {
		super(randomSeed);
		this.scale = scale;
	}

	public double getEpsilon(Id<Person> personId, int tripIndex, Object alternative) {
		double u = getUniformEpsilon(personId, tripIndex, alternative);
		return -scale * Math.log(-Math.log(u));
	}
}
