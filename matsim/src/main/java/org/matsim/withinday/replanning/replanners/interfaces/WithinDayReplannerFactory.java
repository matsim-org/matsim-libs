/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayReplannerFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.replanners.interfaces;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.ActivityEndReschedulerProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentSelector;
import org.matsim.withinday.replanning.replanners.tools.ReplanningIdGenerator;

public abstract class WithinDayReplannerFactory<T extends AgentSelector> {

	private final ActivityEndReschedulerProvider withinDayEngine;
	private Id<WithinDayReplanner> id;
	private Set<T> identifiers = new HashSet<T>();

	public WithinDayReplannerFactory(ActivityEndReschedulerProvider withinDayEngine) {
		this.withinDayEngine = withinDayEngine;
		this.id = ReplanningIdGenerator.getNextId();
	}

	public abstract WithinDayReplanner<? extends AgentSelector> createReplanner();

	public final ActivityEndReschedulerProvider getWithinDayEngine() {
		return this.withinDayEngine;
	}

	public final Id<WithinDayReplanner> getId() {
		return this.id;
	}

	public final boolean addIdentifier(T identifier) {
		return this.identifiers.add(identifier);
	}

	public final boolean removeIdentifier(T identifier) {
		return this.identifiers.remove(identifier);
	}

	public final Set<T> getIdentifers() {
		return Collections.unmodifiableSet(identifiers);
	}
}
