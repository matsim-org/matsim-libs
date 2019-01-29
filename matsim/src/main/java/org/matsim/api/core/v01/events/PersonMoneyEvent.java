/* *********************************************************************** *
 * project: org.matsim.*
 * AgentMoneyEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.api.core.v01.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

/**
 * This event specifies that an agent has gained (or paid) some money.
 * Scoring functions should handle these Events by adding the amount somehow
 * to the score (this can include some kind of normalization with the
 * agent's income or something similar).
 *
 * @author mrieser
 */
public final class PersonMoneyEvent extends Event implements HasPersonId {

	public static final String ATTRIBUTE_AMOUNT = "amount";

	public static final String EVENT_TYPE = "personMoney";
	public static final String ATTRIBUTE_PERSON = "person";

	private final Id<Person> personId;
	private final double amount;

	/**
	 * Creates a new event describing that the given <tt>agent</tt> has <em>gained</em>
	 * some money at the specified <tt>time</tt>. Positive values for <tt>amount</tt>
	 * mean the agent has gained money, negative values that the agent has paid money.
	 *
	 * @param time
	 * @param agentId
	 * @param amount
	 */
	public PersonMoneyEvent(final double time, final Id<Person> agentId, final double amount) {
		super(time);
		this.personId = agentId;
		this.amount = amount;
	}

	public Id<Person> getPersonId() {
		return this.personId;
	}
	
	public double getAmount() {
		return this.amount;
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
		return attr;
	}
}