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

	private static final Integer MAXNUMP = 14;

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
		Household hh = (Household)atts.get(CAtts.HH_W);

		// age
		model.setAge(person.getAge());

		// sex
		if (person.getSex().equals(MALE)) { model.setSex(true); } else { model.setSex(false); }

		// nat
		if (((Integer)atts.get(CAtts.P_HMAT)) == 1) { model.setNationality(true); } else { model.setNationality(false); }

		// nump
		int nump = hh.getPersonsW().size();
		if (nump > MAXNUMP) { nump = MAXNUMP; }
		model.setHHDimension(nump);

		// numk
		double k_frac = hh.getKidsWFraction();
		model.setHHKids((int)Math.round(k_frac*nump));

		// inc
		model.setIncome(hh.getMunicipality().getIncome()/1000.0);

		// udeg
		model.setUrbanDegree(hh.getMunicipality().getRegType());
		
		// calc and assign license ownership
		boolean hasLicense = model.calcLicenseOwnership();
		if (hasLicense) { person.setLicence(YES); } else { person.setLicence(NO); }
		if ((person.getAge() < 18) && (hasLicense)) { person.setLicence(NO); }
	}

	public void run(Plan plan) {
	}
}
