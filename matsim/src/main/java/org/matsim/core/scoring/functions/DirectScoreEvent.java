package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

import java.util.Map;

public class DirectScoreEvent extends Event implements HasPersonId {

	public static final String ATTRIBUTE_AMOUNT = "score";

	public static final String EVENT_TYPE = "directScore";
	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_DESCRIPTION = "description";

	private final Id<Person> personId;
	private final double amount;
	private final String description;

	public DirectScoreEvent(final double time, final Id<Person> agentId, final double amount, final String description) {
		super(time);
		this.personId = agentId;
		this.amount = amount;
		this.description = description;
	}

	public Id<Person> getPersonId() {
		return this.personId;
	}

	public double getScore() {
		return this.amount;
	}

	public String getDescription() {
		return description;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_AMOUNT, Double.toString(this.amount));
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_DESCRIPTION, this.description);
		return attr;
	}

}
