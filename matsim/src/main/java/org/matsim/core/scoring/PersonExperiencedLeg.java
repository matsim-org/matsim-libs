
/* *********************************************************************** *
 * project: org.matsim.*
 * PersonExperiencedLeg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

public final class PersonExperiencedLeg {
	private final Id<Person> agentId;
	private final Leg leg;

	public PersonExperiencedLeg(Id<Person> agentId, Leg leg) {
		this.agentId = agentId;
		this.leg = leg;
	}

	public Id<Person> getAgentId() {
		return agentId;
	}

	public Leg getLeg() {
		return leg;
	}
}
