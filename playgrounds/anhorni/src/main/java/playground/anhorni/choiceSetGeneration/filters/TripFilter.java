/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.choiceSetGeneration.filters;


import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import playground.anhorni.choiceSetGeneration.helper.ChoiceSet;

public abstract class TripFilter {
	
	protected List<ChoiceSet> choiceSets = new Vector<ChoiceSet>();
	private final static Logger log = Logger.getLogger(TripFilter.class);
		
	public List<ChoiceSet> apply(Population population, String mode) {
		for (Person person : population.getPersons().values()) {
			// Person only has one plan at this stage
			Plan plan = person.getSelectedPlan();		
			filterPlan(plan, mode);
		}	
		log.info("Number of "+ mode + " trips :  " + this.choiceSets.size());
		return this.choiceSets;
	}
	
	protected abstract boolean filterPlan(final Plan plan, String mode);
	
}
