/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.mobsim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;

/**
 * @author illenberger
 *
 */
public class PlanAgentBuilder implements MobsimAgentBuilder {

	private static final Logger log = Logger.getLogger(PlanAgentBuilder.class);
	
	private final Plans population;
	
	public PlanAgentBuilder(Plans population) {
		this.population = population;
	}
	
	public List<PlanAgent> buildAgents() {
		List<PlanAgent> agents = new ArrayList<PlanAgent>(population.getPersons().size());
		int countInvalid = 0;
		for(Person p : population) {
			if(validatePerson(p)) {
				agents.add(buildAgent(p));
			} else
				countInvalid++;
		}
		
		if(countInvalid > 0)
			log.warn(String.format(
					"%1$s agents were not build because the person failed validation!",
					countInvalid));
		
		return agents;
	}
	
	protected PlanAgent buildAgent(Person p) {
		return new PlanAgent(p);
	}

	protected boolean validatePerson(Person p) {
		/*
		 * TODO: Do some validation here, e.g., is there a selected plan, is the
		 * selected plan valid (routes)?
		 */
		return true;
	}
}
