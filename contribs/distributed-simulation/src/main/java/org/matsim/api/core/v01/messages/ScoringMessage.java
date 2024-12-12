package org.matsim.api.core.v01.messages;

import lombok.Getter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.dsim.Message;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ScoringMessage implements Message {

	private final Map<Id<Person>, Double> personScores = new HashMap<>();

	public void addScore(Id<Person> personId, double score) {
		personScores.put(personId, score);
	}

}
