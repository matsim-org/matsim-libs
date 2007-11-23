/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLicenseModel.java
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

package playground.balmermi.census2000.modules;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

import playground.balmermi.census2000.data.Persons;
import playground.balmermi.census2000.models.ModelLicenseOwnership;

public class PersonLicenseModel extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String NO = "no";
	private static final String YES = "yes";
	
	private final ModelLicenseOwnership model = new ModelLicenseOwnership();
	private final Persons persons;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonLicenseModel(final Persons persons) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.persons = persons;
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Person person) {
		playground.balmermi.census2000.data.Person p = this.persons.getPerson(Integer.parseInt(person.getId().toString()));
		model.setAge(p.getAge());
		model.setHHDimension(p.getHousehold().getPersonCount());
		model.setHHKids(p.getHousehold().getKidCount());
		model.setIncome(p.getHousehold().getMunicipality().getIncome()/1000.0);
		model.setNationality(p.isSwiss());
		model.setSex(p.isMale());
		model.setUrbanDegree(p.getHousehold().getMunicipality().getRegType());
		boolean hasLicense = model.calcLicenseOwnership();
		if (hasLicense) { person.setLicence(YES); } else { person.setLicence(NO); }
		if ((p.getAge() < 18) && (hasLicense)) {
			person.setLicence(NO);
		}
	}

	public void run(Plan plan) {
	}
}
