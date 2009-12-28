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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
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
//	private final ModelLicenseOwnershipV2 model = new ModelLicenseOwnershipV2();
//	private final ModelLicenseOwnershipV3 model = new ModelLicenseOwnershipV3();
//	private final ModelLicenseOwnershipV4 model = new ModelLicenseOwnershipV4();
//	private final ModelLicenseOwnershipV5 model = new ModelLicenseOwnershipV5();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignLicenseModel() {
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void assignHHSizeModelParamsFromHH(Person p, Household hh) {
		// nump
		int nump = hh.getPersons().size();
		if (nump > MAXNUMP) { nump = MAXNUMP; }
		model.setHHDimension(nump);

		// numk
		double k_frac = hh.getKidsFraction();
		model.setHHKids((int)(0.5+k_frac*nump));
		
		// debug info
		if (hh.getPersons().size() > MAXNUMP) {
			log.debug("FromHH: pid="+p.getId()+": nump("+hh.getPersons().size()+"),numk("+hh.getKids().size()+"),k_frac("+k_frac+") => nump("+nump+"),numk("+((int)(0.5+k_frac*nump))+")");
		}
	}
	
	private final void assignHHSizeModelParamsFromCatts(Person p, Household hh) {
		// nump
		int nump = (Integer)p.getCustomAttributes().get(CAtts.P_APERW);
		if (nump < 1) { nump = (Integer)p.getCustomAttributes().get(CAtts.P_WKATA); }
		if (nump < 1) { Gbl.errorMsg("pid"+p.getId()+": neither '"+CAtts.P_APERW+"' nor '"+CAtts.P_WKATA+"' defined!"); }
		
		if (nump > MAXNUMP) { nump = MAXNUMP; }
		model.setHHDimension(nump);

		// numk
		double k_frac = hh.getKidsWFraction();
		model.setHHKids((int)Math.round(k_frac*nump));
		
		// debug info
		if (hh.getPersonsW().size() > MAXNUMP) {
			log.debug("FromCatts: pid="+p.getId()+": nump("+hh.getPersonsW().size()+"),numk("+hh.getKidsW().size()+") => nump("+nump+"),numk("+((int)Math.round(k_frac*nump))+")");
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Map<String,Object> atts = person.getCustomAttributes();
		Household hh = (Household)atts.get(CAtts.HH_W);

		// age
		model.setAge(((PersonImpl) person).getAge());

		// sex
		if (((PersonImpl) person).getSex().equals(MALE)) { model.setSex(true); } else { model.setSex(false); }

		// nat
		if (((Integer)atts.get(CAtts.P_HMAT)) == 1) { model.setNationality(true); } else { model.setNationality(false); }

//		this.assignHHSizeModelParamsFromCatts(person,hh);
		this.assignHHSizeModelParamsFromHH(person,hh);

		// inc
		model.setIncome(hh.getMunicipality().getIncome()/1000.0);

		// udeg
		model.setUrbanDegree(hh.getMunicipality().getRegType());
		
		// calc and assign license ownership
		boolean hasLicense = false;
		if (((PersonImpl) person).getAge() >= 18) {
			hasLicense = model.calcLicenseOwnership();
			if (!hasLicense && (MatsimRandom.getRandom().nextDouble() < 0.4)) { hasLicense = true; }
		}
		if (hasLicense) { ((PersonImpl) person).setLicence(YES); } else { ((PersonImpl) person).setLicence(NO); }
	}

	public void run(Plan plan) {
	}
}
