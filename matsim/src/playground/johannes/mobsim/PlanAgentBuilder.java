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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

/**
 * A builder class that builds {@link PlanAgent} instances out of {@link PersonImpl}
 * objects.
 * 
 * @author illenberger
 * 
 */
public class PlanAgentBuilder implements MobsimAgentBuilder {
	
	// =======================================================
	// private fields
	// =======================================================

	private static final Logger log = Logger.getLogger(PlanAgentBuilder.class);
	
	private final PopulationImpl population;
	
	// =======================================================
	// constructor
	// =======================================================

	/**
	 * Creates a new builder that builds agents out of the persons in
	 * <tt>population</tt>.
	 * 
	 * @param population
	 *            the population of persons.
	 */
	public PlanAgentBuilder(PopulationImpl population) {
		this.population = population;
	}

	// =======================================================
	// MobsimAgentBuilder implementation
	// =======================================================

	/**
	 * @return a list of newly created {@link PlanAgent} instances built out of
	 *         persons. Each person will be validated by calling
	 *         {@link #validatePerson(PersonImpl)} before the agent is built.
	 */
	public List<PlanAgent> buildAgents() {
		List<PlanAgent> agents = new ArrayList<PlanAgent>(population.getPersons().size());
		int countInvalid = 0;
		for(Person p : population.getPersons().values()) {
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
	
	// =======================================================
	// protected methods to be overwritten by subclasses if needed
	// =======================================================

	/**
	 * Builds a single agent out of <tt>p</tt>. Can be overwritten by
	 * subclasses to build specific agent instances.
	 * 
	 * @param p
	 *            the person which will act as underlying data source.
	 * @return a new {@link PlanAgent} instance.
	 */
	protected PlanAgent buildAgent(Person p) {
		return new PlanAgent(p);
	}

	/**
	 * Validates a person by checking if the person has a selected plan and if
	 * the selected plan does not contain null-routes.
	 * 
	 * @param p
	 *            the person to validate.
	 * @return <tt>true</tt> if the person is valid, <tt>false</tt>
	 *         otherwise.
	 */
	protected boolean validatePerson(Person p) {
		Plan plan = p.getSelectedPlan();
		if(plan == null) {
			return false;
		}
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				if (((LegImpl) pe).getRoute() == null) {
					return false;
				}
			}
		}
		return true;
	}
}
