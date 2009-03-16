/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAddTypicalDurationsToDesires.java
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

package playground.meisterk.org.matsim.population.algorithms;

import java.util.HashMap;

import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PersonAddTypicalDurationsToDesires extends AbstractPersonAlgorithm {

	public static final String APPENDIX = "_typical";
	
	@Override
	public void run(Person person) {
		
		double typicalDuration;
		String actType;
		
		HashMap<String, Integer> numOfOccurrences = new HashMap<String, Integer>();
		
		Plan selectedPlan = person.getSelectedPlan();
		Activity lastActivity = selectedPlan.getLastActivity();
		for (Object o : selectedPlan.getPlanElements()) {
			if ((o instanceof Activity) && !(((Activity) o).equals(lastActivity))) {
				actType = ((Activity) o).getType();
				if (numOfOccurrences.containsKey(actType)) {
					numOfOccurrences.put(actType, numOfOccurrences.get(actType) + 1);
				} else {
					numOfOccurrences.put(actType, 1);
				}
			}
		}
		
		for (String str : numOfOccurrences.keySet()) {
			typicalDuration = person.getDesires().getActivityDuration(str) / numOfOccurrences.get(str);
			person.getDesires().putActivityDuration(str + PersonAddTypicalDurationsToDesires.APPENDIX, typicalDuration);
		}

	}

}
