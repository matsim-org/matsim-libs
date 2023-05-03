package org.matsim.modechoice;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.ScoringParameters;

/**
 * Stores context needed to provide an estimate.
 */
public class EstimatorContext {

	public final Person person;
	public final ScoringParameters scoring;

	public EstimatorContext(Person person, ScoringParameters scoring) {
		this.person = person;
		this.scoring = scoring;
	}
}
