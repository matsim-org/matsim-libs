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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

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

	private Knowledges knowledges;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignMobilitiyToolModel(Knowledges knowledges) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.knowledges = knowledges;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final void assignHHSizeModelParamsFromHH(PersonImpl p, Household hh) {
		// nump
		int nump = hh.getPersons().size();
		if (nump > MAXNUMP) { nump = MAXNUMP; }
		model.setHHDimension(nump);

		// numk
		double k_frac = hh.getKidsFraction();
		model.setHHKids((int)(0.5+k_frac*nump));
		
//		// debug info
//		if (hh.getPersons().size() > MAXNUMP) {
//			log.debug("FromHH: pid="+p.getId()+": nump("+hh.getPersons().size()+"),numk("+hh.getKids().size()+"),k_frac("+k_frac+") => nump("+nump+"),numk("+((int)(0.5+k_frac*nump))+")");
//		}
	}
	
	private final void assignHHSizeModelParamsFromCatts(PersonImpl p, Household hh) {
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
	public void run(Person p) {
		PersonImpl person = (PersonImpl) p;
		Map<String,Object> atts = person.getCustomAttributes();
		Household hh = (Household)atts.get(CAtts.HH_W);
		
		// age
		model.setAge(person.getAge());

		// sex
		if (person.getSex().equals(MALE)) { model.setSex(true); } else { model.setSex(false); }

		// nat
		if (((Integer)atts.get(CAtts.P_HMAT)) == 1) { model.setNationality(true); } else { model.setNationality(false); }

//		this.assignHHSizeModelParamsFromCatts(person,hh);
		this.assignHHSizeModelParamsFromHH(person,hh);
		
		// inc
		model.setIncome(hh.getMunicipality().getIncome()/1000.0);

		// udeg
		model.setUrbanDegree(hh.getMunicipality().getRegType());

		// license
		if (person.getLicense().equals(YES)) { model.setLicenseOwnership(true); } else { model.setLicenseOwnership(false); }

		// disthw
		Coord h_coord = this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_HOME).get(0).getFacility().getCoord();
		ArrayList<ActivityOptionImpl> prim_acts = new ArrayList<ActivityOptionImpl>();
		prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_W2));
		prim_acts.addAll(this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_W3));
		if (prim_acts.isEmpty()) { model.setDistanceHome2Work(0.0); }
		else {
			Coord p_coord = prim_acts.get(MatsimRandom.getRandom().nextInt(prim_acts.size())).getFacility().getCoord();
			model.setDistanceHome2Work(CoordUtils.calcDistance(h_coord, p_coord));
		}

		// fuelcost
		model.setFuelCost(hh.getMunicipality().getFuelCost());

		// language: 0-9 and 11-20 = 1 (German); 10 and 22-26 = 2 (French); 21 = 3 (Italian)
		int cid = hh.getMunicipality().getCantonId();
		if (cid == 21) { model.setLanguage(3); }
		else if (((22 <= cid) && (cid <= 26)) || (cid == 10)) { model.setLanguage(2); }
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
