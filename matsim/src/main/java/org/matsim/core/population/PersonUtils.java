
/* *********************************************************************** *
 * project: org.matsim.*
 * PersonUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import java.util.Iterator;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public final class PersonUtils {
	private PersonUtils(){} // do not instantiate
	
	private final static String SEX_ATTRIBUTE="sex";
	private final static String HAS_LICENSE= "hasLicense";
	private static final String CAR_AVAIL = "carAvail";
	private static final String EMPLOYED = "employed";
	private static final String AGE = "age";
	private static final String TRAVELCARDS = "travelcards";
	private final static Logger log = Logger.getLogger(Person.class);

	@Deprecated // use methods of interface Person
	public static Plan createAndAddPlan(Person person, final boolean selected) {
		Plan p = PopulationUtils.createPlan(person);
		person.addPlan(p);
		if (selected) {
			person.setSelectedPlan(p);
		}
		return p;
	}

	public static void removeUnselectedPlans(Person person) {
		for (Iterator<? extends Plan> iter = person.getPlans().iterator(); iter.hasNext(); ) {
			Plan plan = iter.next();
			if (!PersonUtils.isSelected(plan)) {
				iter.remove();
			}
		}
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static String getSex(Person person) {
		return (String) person.getAttributes().getAttribute(SEX_ATTRIBUTE);
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static Integer getAge(Person person) {
		return (Integer) person.getAttributes().getAttribute(AGE);
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static String getLicense(Person person) {
		return (String) person.getAttributes().getAttribute(HAS_LICENSE);

	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static boolean hasLicense(Person person) {
		return ("yes".equals(getLicense(person))) || ("true".equals(getLicense(person)));
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static String getCarAvail(Person person) {
		return (String) person.getAttributes().getAttribute(CAR_AVAIL);
		}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static Boolean isEmployed(Person person) {
		return (Boolean) person.getAttributes().getAttribute(EMPLOYED);	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static void setAge(Person person, final Integer age) {
		if (age!=null){
		person.getCustomAttributes().put(AGE, age);
		person.getAttributes().putAttribute(AGE,age ) ;
		}
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static void setSex(Person person, final String sex) {
		if (sex!=null){
		person.getCustomAttributes().put(SEX_ATTRIBUTE, sex);
		person.getAttributes().putAttribute( SEX_ATTRIBUTE, sex ) ;
		}
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static void setLicence(Person person, final String licence) {
		if (licence!=null){

		person.getCustomAttributes().put(HAS_LICENSE, licence);
		person.getAttributes().putAttribute(HAS_LICENSE, licence ) ;
		}
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static void setCarAvail(Person person, final String carAvail) {
		if (carAvail!=null){
		person.getCustomAttributes().put(CAR_AVAIL, carAvail);
		person.getAttributes().putAttribute(CAR_AVAIL,carAvail ) ;
		}
	}

	/**
	 * convenience method for often used demographic attribute
	 */
	public static void setEmployed(Person person, final Boolean employed) {
		if (employed!=null){
		person.getCustomAttributes().put(EMPLOYED, employed);
		person.getAttributes().putAttribute(EMPLOYED,employed ) ;
		}
	}

	@Deprecated // yyyy is there a way to use person.getAttributes instead??  kai, nov'16
	public static void addTravelcard(Person person, final String type) {
		if (getTravelcards(person) == null) {
			person.getCustomAttributes().put(TRAVELCARDS, new TreeSet<String>());
		}
		if (getTravelcards(person).contains(type)) {
			log.info(person + "[type=" + type + " already exists]");
		} else {
			getTravelcards(person).add(type.intern());
		}
	}

	@SuppressWarnings("unchecked")
	@Deprecated // use PersonAttributes
	public static TreeSet<String> getTravelcards(Person person) {
		return (TreeSet<String>) person.getCustomAttributes().get(TRAVELCARDS);
	}

	public static boolean isSelected(Plan plan) {
		return plan.getPerson().getSelectedPlan()==plan ;
	}
}
