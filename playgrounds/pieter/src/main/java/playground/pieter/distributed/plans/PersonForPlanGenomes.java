/* *********************************************************************** *
 * project: org.matsim.*
 * Person.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.pieter.distributed.plans;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.population.Desires;
import org.matsim.utils.customize.Customizable;
import org.matsim.utils.customize.CustomizableImpl;

import java.util.*;

/**
 * Default implementation of {@link org.matsim.api.core.v01.population.Person} interface.
 */
public class PersonForPlanGenomes extends PersonImpl implements Person {

	private final static Logger log = Logger.getLogger(PersonForPlanGenomes.class);

    public PersonForPlanGenomes(Id<Person> id) {
        super(id);
    }

    @Override
	public Plan createCopyOfSelectedPlanAndMakeSelected() {
		PlanGenome oldPlan = (PlanGenome) this.getSelectedPlan();
		if (oldPlan == null) {
			return null;
		}
		PlanGenome newPlan = new PlanGenome(oldPlan.getPerson());
		newPlan.copyFrom(oldPlan);
        newPlan.setGenome(oldPlan.getGenome());
        newPlan.setpSimScore(oldPlan.getpSimScore());
		this.getPlans().add(newPlan);
		this.setSelectedPlan(newPlan);
		return newPlan;
	}



}
