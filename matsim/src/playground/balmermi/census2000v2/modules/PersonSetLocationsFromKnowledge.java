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

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.balmermi.census2000v2.data.CAtts;

public class PersonSetLocationsFromKnowledge extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonSetLocationsFromKnowledge.class);
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetLocationsFromKnowledge() {
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		// plan
		if (person.getPlans().size() != 1) { Gbl.errorMsg("pid="+person.getId()+": There must be exactly one plan."); }
		Plan plan = person.getSelectedPlan();
		if (plan == null) { Gbl.errorMsg("pid="+person.getId()+": no plan exists."); }

		// knowledge
		Knowledge k = person.getKnowledge();
		if (k == null) { Gbl.errorMsg("pid="+person.getId()+": no knowledge exists."); }

		// home act
		if (k.getActivities(CAtts.ACT_HOME).size() != 1) { Gbl.errorMsg("pid="+person.getId()+": There must be only one '"+CAtts.ACT_HOME+"' in the knowledge."); }
		Activity home_act = k.getActivities(CAtts.ACT_HOME).get(0);
		
		// work acts
		ArrayList<Activity> work_acts = new ArrayList<Activity>(k.getActivities(CAtts.ACT_W2));
		work_acts.addAll(k.getActivities(CAtts.ACT_W3));
		
		// educ acts
		ArrayList<Activity> educ_acts = new ArrayList<Activity>(k.getActivities(CAtts.ACT_EKIGA));
		educ_acts.addAll(k.getActivities(CAtts.ACT_EPRIM));
		educ_acts.addAll(k.getActivities(CAtts.ACT_ESECO));
		educ_acts.addAll(k.getActivities(CAtts.ACT_EHIGH));
		educ_acts.addAll(k.getActivities(CAtts.ACT_EOTHR));

		Activity prev_home = null;
		Activity prev_work = null;
		Activity prev_educ = null;
		
		for (int i=0; i<plan.getActsLegs().size(); i++) {
			if (i%2 == 0) {
				Act act = (Act)plan.getActsLegs().get(i);
				if (act.getType().startsWith("h")) {
					if (prev_home != null) { log.warn("TODO pid="+person.getId()+": Two home acts in a row. Not sure yet how to handle that..."); }
					act.setType(home_act.getType());
					act.setFacility(home_act.getFacility());
					act.setCoord(act.getFacility().getCenter());
					prev_home = home_act;
					prev_work = null;
					prev_educ = null;
				}
				else if (act.getType().startsWith("w")) {
					if (work_acts.isEmpty()) { Gbl.errorMsg("pid="+person.getId()+": plan contains 'w' act but no location known!"); }
					Activity work_act = null;
					if (prev_work != null) {
						ArrayList<Activity> rest = new ArrayList<Activity>(work_acts);
						rest.remove(prev_work);
						if (rest.isEmpty()) { work_act = prev_work; log.warn("TODO pid="+person.getId()+": assign another work act."); }
						else { work_act = rest.get(MatsimRandom.random.nextInt(rest.size())); }
					}
					else {
						work_act = work_acts.get(MatsimRandom.random.nextInt(work_acts.size()));
					}
					act.setType(work_act.getType());
					act.setFacility(work_act.getFacility());
					act.setCoord(act.getFacility().getCenter());
					prev_home = null;
					prev_work = work_act;
					prev_educ = null;
				}
				else if (act.getType().startsWith("e")) {
					if (educ_acts.isEmpty()) { Gbl.errorMsg("pid="+person.getId()+": plan contains 'e' act but no location known!"); }
					Activity educ_act = null;
					if (prev_educ != null) {
						ArrayList<Activity> rest = new ArrayList<Activity>(educ_acts);
						rest.remove(prev_educ);
						if (rest.isEmpty()) { educ_act = prev_educ; log.warn("TODO pid="+person.getId()+": assign another educ act."); }
						else { educ_act = rest.get(MatsimRandom.random.nextInt(rest.size())); }
					}
					else {
						educ_act = educ_acts.get(MatsimRandom.random.nextInt(educ_acts.size()));
					}
					act.setType(educ_act.getType());
					act.setFacility(educ_act.getFacility());
					act.setCoord(act.getFacility().getCenter());
					prev_home = null;
					prev_work = null;
					prev_educ = educ_act;
				}
				else {
					prev_home = null;
					prev_work = null;
					prev_educ = null;
				}
			}
		}
	}
}
