/* *********************************************************************** *
 * project: org.matsim.*
 * DecisionMakerImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.agentsmating.logitbasedmating.framework.DecisionMaker;
import playground.thibautd.agentsmating.logitbasedmating.framework.UnexistingAttributeException;

/**
 * Default implementation of a DecisionMaker
 * @author thibautd
 */
public class DecisionMakerImpl implements DecisionMaker {
	private final AttributesMap attributes = new AttributesMap();
	private final Id personId;

	public DecisionMakerImpl(
			final Id personId,
			final Map<String, ? extends Object> attributes) {
		this.attributes.setAttributes(attributes);
		this.personId = personId;
	}

	public DecisionMakerImpl(
			final PersonImpl person) {
		personId = person.getId();
		attributes.setAttributes(person.getCustomAttributes());
	}

	@Override
	public double getAttribute(final String attribute)
			throws UnexistingAttributeException {
		return attributes.getAttribute(attribute);
	}

	@Override
	public Map<String, Object> getAttributes() {
		return attributes.getAttributes();
	}

	@Override
	public Id getPersonId() {
		return personId;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"={ personId="+personId+
			" , "+attributes+" }";
	}
}

