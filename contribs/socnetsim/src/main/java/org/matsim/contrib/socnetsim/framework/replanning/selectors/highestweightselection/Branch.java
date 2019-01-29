/* *********************************************************************** *
 * project: org.matsim.*
 * Branch.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors.highestweightselection;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author thibautd
 */
final class Branch {
	private final Set<Id<Person>> cotravelers;
	private final Set<Id> incompatibilityGroups;

	public Branch(
			final Set<Id<Person>> cotravs,
			final Set<Id> incompatibilityGroups) {
		this.cotravelers = cotravs;
		this.incompatibilityGroups = incompatibilityGroups;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof Branch &&
			((Branch) o).cotravelers.equals( cotravelers ) &&
			((Branch) o).incompatibilityGroups.equals( incompatibilityGroups );
	}

	@Override
	public int hashCode() {
		return cotravelers.hashCode() + incompatibilityGroups.hashCode();
	}
}

