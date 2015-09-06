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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
/**
 * Default implementation of {@link Person} interface.
 */
public class PersonImplPops extends PersonImpl {

	public static final String DEFAULT_POP = "-1";
	public static final Id DEFAULT_POP_ID = Id.create(DEFAULT_POP,Population.class);
	
	private Id<Population> populationId;

	public PersonImplPops(Id id) {
		super(id);
		this.populationId = Id.create(DEFAULT_POP,Population.class);
	}
	public PersonImplPops(Id id, Id<Population> populationId) {
		super(id);
		this.populationId = populationId==null?Id.create(DEFAULT_POP,Population.class):populationId;
	}
	public PersonImplPops(Person person, Id populationId) {
		super(person.getId());
		setAge(this, PersonImpl.getAge(person));
		setCarAvail(this, PersonImpl.getCarAvail(person));
		setEmployed(this, PersonImpl.isEmployed(person));
		setLicence(this, PersonImpl.getLicense(person));
		this.populationId = populationId==null?Id.create(DEFAULT_POP,Population.class):populationId;
		setSex(this, PersonImpl.getSex(person));
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

}
