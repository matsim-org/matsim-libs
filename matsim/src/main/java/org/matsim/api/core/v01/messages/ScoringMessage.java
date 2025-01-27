package org.matsim.api.core.v01.messages;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.population.Person;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds scores for persons.
 */
public class ScoringMessage implements Message {

	private final Map<Id<Person>, Double> personScores = new HashMap<>();

	public void addScore(Id<Person> personId, double score) {
		personScores.put(personId, score);
	}

	public Map<Id<Person>, Double> getPersonScores() {
		return this.personScores;
	}
}
