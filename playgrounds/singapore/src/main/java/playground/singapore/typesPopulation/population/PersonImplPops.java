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

package playground.singapore.typesPopulation.population;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;

/**
 * Default implementation of {@link Person} interface.
 */
public class PersonImplPops implements Person {
	private final Person delegate;

	public static final String DEFAULT_POP = "-1";
	public static final Id DEFAULT_POP_ID = Id.create(DEFAULT_POP,Population.class);
	
	private Id<Population> populationId;

	public PersonImplPops(Id id) {
		delegate = PersonImpl.createPerson(id);
		this.populationId = Id.create(DEFAULT_POP,Population.class);
	}
	public PersonImplPops(Id id, Id<Population> populationId) {
		delegate = PersonImpl.createPerson(id);
		this.populationId = populationId==null?Id.create(DEFAULT_POP,Population.class):populationId;
	}
	public PersonImplPops(Person person, Id populationId) {
		delegate = PersonImpl.createPerson(person.getId());
		PersonUtils.setAge(this, PersonUtils.getAge(person));
		PersonUtils.setCarAvail(this, PersonUtils.getCarAvail(person));
		PersonUtils.setEmployed(this, PersonUtils.isEmployed(person));
		PersonUtils.setLicence(this, PersonUtils.getLicense(person));
		this.populationId = populationId==null?Id.create(DEFAULT_POP,Population.class):populationId;
		PersonUtils.setSex(this, PersonUtils.getSex(person));
		for(Plan plan:person.getPlans())
			addPlan(plan);
		setSelectedPlan(person.getSelectedPlan());
	}
	public Id<Population> getPopulationId() {
		return populationId;
	}

	public void setPopulationId(Id<Population> populationId) {
		this.populationId = populationId;
	}
		public List<? extends Plan> getPlans() {
			return delegate.getPlans();
		}
		public boolean addPlan(Plan p) {
			return delegate.addPlan(p);
		}
		public boolean removePlan(Plan p) {
			return delegate.removePlan(p);
		}
		public Plan getSelectedPlan() {
			return delegate.getSelectedPlan();
		}
		public void setSelectedPlan(Plan selectedPlan) {
			delegate.setSelectedPlan(selectedPlan);
		}
		public Plan createCopyOfSelectedPlanAndMakeSelected() {
			return delegate.createCopyOfSelectedPlanAndMakeSelected();
		}
		public Id<Person> getId() {
			return delegate.getId();
		}
		public Map<String, Object> getCustomAttributes() {
			return delegate.getCustomAttributes();
		}

}
