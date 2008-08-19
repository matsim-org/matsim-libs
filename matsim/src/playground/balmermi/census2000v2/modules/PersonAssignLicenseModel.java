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

package playground.balmermi.census2000v2.modules;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000.models.ModelLicenseOwnership;
import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;

public class PersonAssignLicenseModel extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignLicenseModel.class);

	private static final String MALE = "m";
	private static final String NO = "no";
	private static final String YES = "yes";
	private final ModelLicenseOwnership model = new ModelLicenseOwnership();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignLicenseModel() {
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Map<String,Object> atts = person.getCustomAttributes();
		
		model.setAge(person.getAge());
		if (person.getSex().equals(MALE)) { model.setSex(true); }
		if (((Integer)atts.get(CAtts.P_HMAT)) == 1) { model.setNationality(true); }
		Object o = atts.get(CAtts.HH_W);
		if (o == null) { o = atts.get(CAtts.HH_Z); }
		Household hh = (Household)o;
		model.setIncome(hh.getMunicipality().getIncome()/1000.0);
		model.setUrbanDegree(hh.getMunicipality().getRegType());
		
		Map<Id,Person> persons = hh.getPersons();
		model.setHHDimension(persons.size());
		int kids = 0;
		for (Person p : persons.values()) { if (p.getAge() < 15) { kids++; } }
		model.setHHKids(kids);
		boolean hasLicense = model.calcLicenseOwnership();
		if (hasLicense) { person.setLicence(YES); } else { person.setLicence(NO); }
		if ((person.getAge() < 18) && (hasLicense)) {
			person.setLicence(NO);
		}
	}

	public void run(Plan plan) {
	}
}
