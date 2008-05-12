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

import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.facilities.algorithms.FacilityAlgorithmI;

/**
 * Keeps all facilities if they contain one OR more of the specified activities.
 * 
 * @author meisterk
 *
 */
public class FacilitiesActTypeFilter extends AbstractFacilityFilter {

	private Set<String> actTypePatterns = new TreeSet<String>();

	public FacilitiesActTypeFilter(FacilityAlgorithmI nextAlgorithm) {
		super();
		this.nextAlgorithm = nextAlgorithm;
	}

	public void addActTypePattern(String actTypePattern) {
		this.actTypePatterns.add(actTypePattern);
	}

	public boolean judge(Facility facility) {

		Iterator<String> activityIterator = facility.getActivities().keySet().iterator();
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

	@Override
	public void run(Facilities facilities) {
		// TODO Auto-generated method stub
	}

}
