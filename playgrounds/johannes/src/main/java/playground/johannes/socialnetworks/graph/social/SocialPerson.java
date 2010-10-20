/* *********************************************************************** *
 * project: org.matsim.*
 * SocialPerson.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.social;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.PersonImpl;

/**
 * @author illenberger
 *
 */
public class SocialPerson {

	private PersonImpl person;
	
	private String name;
	
	private String citizenship;
	
	private String education;
	
	private int income;
	
	private String civilStatus;
	
	public SocialPerson(PersonImpl person) {
		this.person = person;
	}
	
	public Id getId() {
		return person.getId();
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getAge() {
		return person.getAge();
	}
	
	public String getCitizenship() {
		return citizenship;
	}
	
	public void setCitizenship(String citizenship) {
		this.citizenship = citizenship;
	}
	
	public String getEducation() {
		return education;
	}
	
	public void setEducation(String edu) {
		this.education = edu;
	}

	public int getIncome() {
		return income;
	}
	
	public void setIncome(int income) {
		this.income = income;
	}

	public String getCiviStatus() {
		return civilStatus;
	}
	
	public void setCivilStatus(String civilStatus) {
		this.civilStatus = civilStatus;
	}
	
	public PersonImpl getPerson() {
		return person;
	}
}
