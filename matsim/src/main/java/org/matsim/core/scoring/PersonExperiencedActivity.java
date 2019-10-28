
/* *********************************************************************** *
 * project: org.matsim.*
 * PersonExperiencedActivity.java
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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;

public final class PersonExperiencedActivity {
	private final Id<Person> agentId;
	private final Activity activity;

	public PersonExperiencedActivity(Id<Person> agentId, Activity activity) {
		this.agentId = agentId;
		this.activity = activity;
	}

	public Id<Person> getAgentId() {
		return agentId;
	}

	public Activity getActivity() {
		return activity;
	}
}
