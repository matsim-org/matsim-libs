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

package org.matsim.core.population;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.CustomizableUtils;
import org.matsim.population.Desires;
/**
 * Default implementation of {@link Person} interface.
 */
public class PersonImpl implements Person {

	private final static String SEX_ATTRIBUTE="sex";
	private final static String HAS_LICENSE= "hasLicense";
	private static final String CAR_AVAIL = "carAvail";
	private static final String EMPLOYED = "employed";
	private static final String AGE = "age";
	private static final String TRAVELCARDS = "travelcards";

	private final static Logger log = Logger.getLogger(PersonImpl.class);

	protected List<Plan> plans = new ArrayList<Plan>(6);
	protected Id<Person> id;

	protected Desires desires = null;

	private Plan selectedPlan = null;

	private Customizable customizableDelegate;

	@Deprecated // please try to use the factory: pop.getFactory().create...
	protected PersonImpl(final Id<Person> id) {
		this.id = id;
	}

	public static Person createPerson(final Id<Person> id) {
		return new PersonImpl(id);
	}

	@Override
	public final Plan getSelectedPlan() {
		return this.selectedPlan;
	}

	@Override
	public boolean addPlan(final Plan plan) {
		plan.setPerson(this);
		// Make sure there is a selected plan if there is at least one plan
		if (this.selectedPlan == null) this.selectedPlan = plan;
		return this.plans.add(plan);
	}

	@Deprecated // use methods of interface Person
	public static PlanImpl createAndAddPlan(Person person, final boolean selected) {
		PlanImpl p = new PlanImpl(person);
		person.addPlan(p);
		if (selected) {
			person.setSelectedPlan(p);
		}
		return p;
	}

	@Override
	public final void setSelectedPlan(final Plan selectedPlan) {
		if (selectedPlan != null && !plans.contains( selectedPlan )) {
			throw new IllegalStateException("The plan to be set as selected is not null nor stored in the person's plans");
		}
		this.selectedPlan = selectedPlan;
	}

	public static void removeUnselectedPlans(Person person) {
		for (Iterator<? extends Plan> iter = person.getPlans().iterator(); iter.hasNext(); ) {
			Plan plan = iter.next();
			if (!plan.isSelected()) {
				iter.remove();
			}
		}
	}

	@Override
	public Plan createCopyOfSelectedPlanAndMakeSelected() {
		Plan oldPlan = this.getSelectedPlan();
		if (oldPlan == null) {
			return null;
		}
		PlanImpl newPlan = new PlanImpl(oldPlan.getPerson());
		newPlan.copyFrom(oldPlan);
		this.getPlans().add(newPlan);
		this.setSelectedPlan(newPlan);
		return newPlan;
	}

	@Override
	public Id<Person> getId() {
		return this.id;
	}

    // Not on interface. Only to be used for demand generation.
	public void setId(final Id<Person> id) {
		this.id = id;
	}

	@Deprecated // use PersonAttributes
	public static String getSex(Person person) {
		return (String) person.getCustomAttributes().get(SEX_ATTRIBUTE);
	}

	@Deprecated // use PersonAttributes
	public static Integer getAge(Person person) {
		return (Integer) person.getCustomAttributes().get(AGE);
	}

	@Deprecated // use PersonAttributes
	public static String getLicense(Person person) {
		return (String) person.getCustomAttributes().get(HAS_LICENSE);
	}

	@Deprecated // use PersonAttributes
	public static boolean hasLicense(Person person) {
		return ("yes".equals(PersonImpl.getLicense(person))) || ("true".equals(PersonImpl.getLicense(person)));
	}

	@Deprecated // use PersonAttributes
	public static String getCarAvail(Person person) {
		return (String) person.getCustomAttributes().get(CAR_AVAIL);
	}

	@Deprecated // use PersonAttributes
	public static Boolean isEmployed(Person person) {
		return (Boolean) person.getCustomAttributes().get(EMPLOYED);
	}

	@Deprecated // use PersonAttributes
	public static void setAge(Person person, final Integer age) {
		person.getCustomAttributes().put(AGE, age);
	}

	@Deprecated // use PersonAttributes
	public static void setSex(Person person, final String sex) {
		person.getCustomAttributes().put(SEX_ATTRIBUTE, sex);
	}

	@Deprecated // use PersonAttributes
	public static void setLicence(Person person, final String licence) {
		person.getCustomAttributes().put(HAS_LICENSE, licence);
	}

	@Deprecated // use PersonAttributes
	public static void setCarAvail(Person person, final String carAvail) {
		person.getCustomAttributes().put(CAR_AVAIL, carAvail);
	}

	@Deprecated // use PersonAttributes
	public static void setEmployed(Person person, final Boolean employed) {
		person.getCustomAttributes().put(EMPLOYED, employed);
	}

	@Deprecated // use PersonAttributes
	public final Desires createDesires(final String desc) {
		if (this.desires == null) {
			this.desires = new Desires(desc);
		}
		return this.desires;
	}


	@Deprecated // use PersonAttributes
	public static void addTravelcard(Person person, final String type) {
		if (PersonImpl.getTravelcards(person) == null) {
			person.getCustomAttributes().put(TRAVELCARDS, new TreeSet<String>());
		}
		if (PersonImpl.getTravelcards(person).contains(type)) {
			log.info(person + "[type=" + type + " already exists]");
		} else {
			PersonImpl.getTravelcards(person).add(type.intern());
		}
	}


	@Deprecated // use PersonAttributes
	public static TreeSet<String> getTravelcards(Person person) {
		return (TreeSet<String>) person.getCustomAttributes().get(TRAVELCARDS);
	}


	@Deprecated // use PersonAttributes
	public final Desires getDesires() {
		return this.desires;
	}




	@Override
	public final String toString() {
		StringBuilder b = new StringBuilder();
		b.append("[id=").append(this.getId()).append("]");
		b.append("[nof_plans=").append(this.getPlans() == null ? "null" : this.getPlans().size()).append("]");
		return b.toString();
	}

	@Override
	public boolean removePlan(final Plan plan) {
		boolean result = this.getPlans().remove(plan);
		if ((this.getSelectedPlan() == plan) && result) {
			this.setSelectedPlan(new RandomPlanSelector<Plan, Person>().selectPlan(this));
		}
		return result;
	}

	@Override
	public List<Plan> getPlans() {
		return this.plans;
	}


	@Override
	public Map<String, Object> getCustomAttributes() {
		if (this.customizableDelegate == null) {
			this.customizableDelegate = CustomizableUtils.createCustomizable();
		}
		return this.customizableDelegate.getCustomAttributes();
	}

}
