/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.noise.personLinkMoneyEvents;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

/**
 * The idea is to have a standard events format to be used by various external cost pricing approaches.
 * 
 * The normal {@link PersonMoneyEvent} is not sufficient since we need refer the toll to a specific link
 * and the link information may not be available when the money event is thrown. Also, the routing relevant time
 * may be different from the time when the event is thrown.
 * 
 * @author ikaddoura
*/

public class PersonLinkMoneyEvent extends Event implements HasPersonId{

	public static final String ATTRIBUTE_AMOUNT = "amount";

	public static final String EVENT_TYPE = "personLinkMoney";
	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_RELEVANT_TIME = "relevantTime";
	public static final String ATTRIBUTE_DESCRIPTION = "description";
	
	private final Id<Person> personId;
	private final Id<Link> linkId;
	private final double amount;
	private final double relevantTime;
	private final String description;
	
	public PersonLinkMoneyEvent(final double time, final Id<Person> agentId, final Id<Link> linkId, final double amount, final double relevantTime, final String description) {
		super(time);
		this.personId = agentId;
		this.linkId = linkId;
		this.amount = amount;
		this.relevantTime = relevantTime;
		this.description = description;
	}

	public Id<Person> getPersonId() {
		return this.personId;
	}
	
	public double getAmount() {
		return this.amount;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public double getRelevantTime() {
		return relevantTime;
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
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_RELEVANT_TIME, Double.toString(this.relevantTime));
		attr.put(ATTRIBUTE_DESCRIPTION, this.description);
		return attr;
	}

}

