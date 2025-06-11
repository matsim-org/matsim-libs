/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.XmlUtils;

import java.util.Map;

/**
 * This event specifies that an agent has gained (or lost) some score-utility.
 * Scoring functions should handle these Events by adding the amount to the agents score.
 *
 * @author mrieser / Simunto
 */
public class PersonScoreEvent extends Event implements HasPersonId {

	public static final String EVENT_TYPE = "personScore";

	public static final String ATTRIBUTE_AMOUNT = "amount";
	public static final String ATTRIBUTE_KIND = "kind";

	private final Id<Person> personId;
	private final double amount;
	private final String kind;

	public PersonScoreEvent(double time, Id<Person> personId, double amount, String kind) {
		super(time);
		this.personId = personId;
		this.amount = amount;
		this.kind = kind;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	public double getAmount() {
		return this.amount;
	}

	public String getKind() {
		return this.kind;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		// personId is added by super-class
		attr.put(ATTRIBUTE_AMOUNT, Double.toString(this.amount));
		if (this.kind != null) {
			attr.put(ATTRIBUTE_KIND, this.kind);
		}
		return attr;
	}

	@Override
	public void writeAsXML(StringBuilder out) {
		writeXMLStart(out);
		out.append("amount=\"").append(this.amount).append("\" ");
		if (this.kind != null) {
			XmlUtils.writeEncodedAttributeKeyValue(out, ATTRIBUTE_KIND, this.kind);
		}
		writeXMLEnd(out);
	}
}
