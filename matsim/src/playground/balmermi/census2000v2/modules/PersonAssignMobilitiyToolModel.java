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
import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
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
		
		// get home activity
		Activity home_act = null;
		if (person.getKnowledge().getActivities(CAtts.ACT_HOME).size() < 1) {
			Gbl.errorMsg("pid="+person.getId()+": no "+CAtts.ACT_HOME+" activity defined.");
		}
		else if (person.getKnowledge().getActivities(CAtts.ACT_HOME).size() == 1) {
			home_act = person.getKnowledge().getActivities(CAtts.ACT_HOME).get(0);
		}
		else if (person.getKnowledge().getActivities(CAtts.ACT_HOME).size() == 2) {
			Gbl.errorMsg("pid="+person.getId()+", home_facid="+home_act.getFacility().getId()+": It should not reach that line anymore.");
			Household hh = (Household)person.getCustomAttributes().get(CAtts.HH_W);
			home_act = hh.getFacility().getActivity(CAtts.ACT_HOME);
			// consistency check
			if (!person.getKnowledge().getActivities(CAtts.ACT_HOME).get(0).equals(home_act) &&
			    !person.getKnowledge().getActivities(CAtts.ACT_HOME).get(1).equals(home_act)) {
				Gbl.errorMsg("pid="+person.getId()+", home_facid="+home_act.getFacility().getId()+": Facility is inconsistent with home activities given in the knowledge.");
			}
		}
		else {
			Gbl.errorMsg("pid="+person.getId()+": more than two "+CAtts.ACT_HOME+" activity defined.");
		}

		// get primary activity
		Activity prim_act = null;
		ArrayList<Activity> prim_acts = new ArrayList<Activity>();
		prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_W2));
		prim_acts.addAll(person.getKnowledge().getActivities(CAtts.ACT_W3));
		if (!prim_acts.isEmpty()) {
			double dist = Double.NEGATIVE_INFINITY;
			for (Activity act : prim_acts) {
				Coord coord = act.getFacility().getCenter();
				double curr_dist = coord.calcDistance(home_act.getFacility().getCenter());
				if (curr_dist > dist) { dist = curr_dist; prim_act = act; }
			}
		}
		
		// calc distance home<==>prim activity
		double distance = 0.0;
		if (prim_act != null) { distance = prim_act.getFacility().getCenter().calcDistance(home_act.getFacility().getCenter()); }
		
		// get infos
		Map<String,Object> atts = person.getCustomAttributes();
		Object o = atts.get(CAtts.HH_W);
		if (o == null) { Gbl.errorMsg("pid="+person.getId()+": no '"+CAtts.HH_W+" defined."); }
		Household hh = (Household)o;
		Map<Id,Person> persons = hh.getPersons();
		int cid = hh.getMunicipality().getCantonId();
		
		// set model parameters
		model.setAge(person.getAge());
		if (person.getSex().equals(MALE)) { model.setSex(true); } else { model.setSex(false); }
		if (((Integer)atts.get(CAtts.P_HMAT)) == 1) { model.setNationality(true); } else { model.setNationality(false); }

		int nump = persons.size();
		int kids = 0; for (Person p : persons.values()) { if (p.getAge() < 15) { kids++; } }
		if (nump > MAXNUMP) {
//			log.trace("pid="+person.getId()+": numpHH="+persons.size()+", kidsHH="+kids);
			kids = Math.round(MAXNUMP*kids/nump);
			nump = MAXNUMP;
//			log.trace("=> numpHH="+nump+", kidsHH="+kids);
		}
		model.setHHDimension(nump);
		model.setHHKids(kids);

		model.setIncome(hh.getMunicipality().getIncome()/1000.0);
		model.setUrbanDegree(hh.getMunicipality().getRegType());
		if (person.getLicense().equals(YES)) { model.setLicenseOwnership(true); } else { model.setLicenseOwnership(false); }
		model.setDistanceHome2Work(distance);
		model.setFuelCost(hh.getMunicipality().getFuelCost());
		// 0-9 and 11-20 = 1 (German); 10 and 22-26 = 2 (French); 21 = 3 (Italian) 
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
