/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesActTypeFilter.java
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

package org.matsim.facilities.filters;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.FacilityAlgorithm;

/**
 * Keeps all facilities if they contain one OR more of the specified activities.
 *
 * @author meisterk
 *
 */
public class FacilitiesActTypeFilter extends AbstractFacilityFilter {

	private final Set<String> actTypePatterns = new TreeSet<String>();

	public FacilitiesActTypeFilter(final FacilityAlgorithm nextAlgorithm) {
		super();
		this.nextAlgorithm = nextAlgorithm;
	}

	public void addActTypePattern(final String actTypePattern) {
		this.actTypePatterns.add(actTypePattern);
	}

	@Override
	public boolean judge(final ActivityFacility facility) {

		Iterator<String> activityIterator = facility.getActivityOptions().keySet().iterator();
		while (activityIterator.hasNext()) {
			String activity = activityIterator.next();
			for (String actTypePattern : this.actTypePatterns) {
				if (Pattern.matches(actTypePattern, activity)) {
					return true;
				}
			}
		}
		return false;
	}

}
