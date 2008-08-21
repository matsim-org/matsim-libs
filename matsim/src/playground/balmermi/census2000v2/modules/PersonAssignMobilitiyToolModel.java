/* *********************************************************************** *
 * project: org.matsim.*
 * PersonMobilityToolModel.java
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

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.facilities.Activity;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.geometry.Coord;

import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;
import playground.balmermi.census2000v2.models.ModelMobilityTools;

public class PersonAssignMobilitiyToolModel extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignMobilitiyToolModel.class);

	private static final Integer MAXNUMP = 14;
	
	private static final String MALE = "m";
	private static final String YES = "yes";

	private static final String ALWAYS = "always";
	private static final String SOMETIMES = "sometimes";
	private static final String NEVER = "never";
	private static final String UNKNOWN = "unknown";

	private final ModelMobilityTools model = new ModelMobilityTools();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignMobilitiyToolModel() {
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

		// license
		if (person.getLicense().equals(YES)) { model.setLicenseOwnership(true); } else { model.setLicenseOwnership(false); }

		// disthw
		Coord h_coord = person.getKnowledge().getActivities(CAtts.ACT_HOME).get(0).getFacility().getCenter();
		ArrayList<Activity> prim_acts = new ArrayList<Activity>();
		prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_W2));
		prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_W3));
		Coord p_coord = prim_acts.get(MatsimRandom.random.nextInt(prim_acts.size())).getFacility().getCenter();
		model.setDistanceHome2Work(h_coord.calcDistance(p_coord));

		// fuelcost
		model.setFuelCost(hh.getMunicipality().getFuelCost());

		// language: 0-9 and 11-20 = 1 (German); 10 and 22-26 = 2 (French); 21 = 3 (Italian)
		int cid = hh.getMunicipality().getCantonId();
		if (cid == 21) { model.setLanguage(3); }
		else if ((22 <= cid) && (cid <= 26) || (cid == 10)) { model.setLanguage(2); }
		else { model.setLanguage(1); }

		// calc and assign mobility tools
		int mobtype = model.calcMobilityTools();
		if ((3 <= mobtype) && (mobtype <= 5)) { person.addTravelcard(UNKNOWN); }
		person.setCarAvail(null);
		if ((0 == mobtype) || (mobtype == 3)) { person.setCarAvail(NEVER); }
		if ((1 == mobtype) || (mobtype == 4)) { person.setCarAvail(SOMETIMES); }
		if ((2 == mobtype) || (mobtype == 5)) { person.setCarAvail(ALWAYS); }
	}

	public void run(Plan plan) {
	}
}
