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
package playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector;

import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * @author thibautd
 */
final class Branch {
	private final Set<Id> cotravelers, incompatibilityGroups;

	public Branch(
			final Set<Id> cotravelers,
			final Set<Id> incompatibilityGroups) {
		this.cotravelers = cotravelers;
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

