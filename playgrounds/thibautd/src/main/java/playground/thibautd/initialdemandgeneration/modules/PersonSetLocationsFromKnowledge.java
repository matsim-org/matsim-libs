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

package playground.thibautd.initialdemandgeneration.modules;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

import playground.thibautd.initialdemandgeneration.CAtts;

/**
 * For <u>primary</u> activities only.
 *
 * <ul>
 * <li> it affects facility, coordinates and activity types from the corresponding
 * activity options in the knowledges.
 * <li> if the activity as preceded by an activity of a different type, an activity
 * option is picked at random from the knowledge
 * <li> otherwise, if other options exist, the choice is restricted to this subset
 * </ul>
 */
public class PersonSetLocationsFromKnowledge extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonSetLocationsFromKnowledge.class);
	private final Knowledges knowledges;
	private final ActivityFacilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetLocationsFromKnowledge(
			final Knowledges knowledges,
			final ActivityFacilities facilities) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.knowledges = knowledges;
		this.facilities = facilities;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		// plan
		if (person.getPlans().size() != 1) {
			throw new RuntimeException("pid="+person.getId()+": There must be exactly one plan.");
		}

		Plan plan = person.getSelectedPlan();
		if (plan == null) {
			throw new RuntimeException("pid="+person.getId()+": no plan exists.");
		}

		// knowledge
		KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(person.getId());
		if (k == null) {
			throw new RuntimeException("pid="+person.getId()+": no knowledge exists.");
		}

		// home act
		if (k.getActivities(CAtts.ACT_HOME).size() != 1) {
			throw new RuntimeException("pid="+person.getId()+": There must be only one '"+CAtts.ACT_HOME+"' in the knowledge.");
		}
		ActivityOptionImpl home_act = k.getActivities(CAtts.ACT_HOME).get(0);

		// work acts
		ArrayList<ActivityOptionImpl> work_acts = new ArrayList<ActivityOptionImpl>(k.getActivities(CAtts.ACT_W2));
		work_acts.addAll(k.getActivities(CAtts.ACT_W3));

		// educ acts
		ArrayList<ActivityOptionImpl> educ_acts = new ArrayList<ActivityOptionImpl>();
		educ_acts.addAll( k.getActivities(CAtts.ACT_EKIGA) );
		educ_acts.addAll( k.getActivities(CAtts.ACT_EPRIM) );
		educ_acts.addAll( k.getActivities(CAtts.ACT_ESECO) );
		educ_acts.addAll( k.getActivities(CAtts.ACT_EHIGH) );
		educ_acts.addAll( k.getActivities(CAtts.ACT_EOTHR) );

		ActivityOptionImpl prev_home = null;
		ActivityOptionImpl prev_work = null;
		ActivityOptionImpl prev_educ = null;

		for (int i=0; i<plan.getPlanElements().size(); i++) {
			PlanElement pe = plan.getPlanElements().get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (act.getType().startsWith("h")) {
					if (prev_home != null) {
						log.warn("TODO pid="+person.getId()+
								": Two home acts in a row. Not sure yet how to handle that...");
					}
					act.setType(home_act.getType());
					if (act instanceof ActivityImpl) {
						((ActivityImpl) act).setFacilityId(home_act.getFacility().getId());
						((ActivityImpl) act).setCoord(this.facilities.getFacilities().get(act.getFacilityId()).getCoord());
					}
					prev_home = home_act;
					prev_work = null;
					prev_educ = null;
				}
				else if (act.getType().startsWith("w")) {
					if (work_acts.isEmpty()) { throw new RuntimeException("pid="+person.getId()+": plan contains '"+act.getType()+"' act but no work location known!"); }
					ActivityOptionImpl work_act = null;
					if (prev_work != null) {
						ArrayList<ActivityOptionImpl> rest = new ArrayList<ActivityOptionImpl>(work_acts);
						rest.remove(prev_work);
						if (rest.isEmpty()) { work_act = prev_work; log.warn("TODO pid="+person.getId()+": assign another work act."); }
						else { work_act = rest.get(MatsimRandom.getRandom().nextInt(rest.size())); }
					}
					else {
						work_act = work_acts.get(MatsimRandom.getRandom().nextInt(work_acts.size()));
					}
					act.setType(work_act.getType());
					if (act instanceof ActivityImpl) {
						((ActivityImpl) act).setFacilityId(work_act.getFacility().getId());
						((ActivityImpl) act).setCoord(this.facilities.getFacilities().get(act.getFacilityId()).getCoord());
					}
					prev_home = null;
					prev_work = work_act;
					prev_educ = null;
				}
				else if (act.getType().startsWith("e")) {
					if (educ_acts.isEmpty()) { throw new RuntimeException("pid="+person.getId()+": plan contains '"+act.getType()+"' act but no education location known!"); }
					ActivityOptionImpl educ_act = null;
					if (prev_educ != null) {
						ArrayList<ActivityOptionImpl> rest = new ArrayList<ActivityOptionImpl>(educ_acts);
						rest.remove(prev_educ);
						if (rest.isEmpty()) { educ_act = prev_educ; log.warn("TODO pid="+person.getId()+": assign another educ act."); }
						else { educ_act = rest.get(MatsimRandom.getRandom().nextInt(rest.size())); }
					}
					else {
						educ_act = educ_acts.get(MatsimRandom.getRandom().nextInt(educ_acts.size()));
					}
					act.setType(educ_act.getType());
					if (act instanceof ActivityImpl) {
						((ActivityImpl) act).setFacilityId(educ_act.getFacility().getId());
						((ActivityImpl) act).setCoord(this.facilities.getFacilities().get(act.getFacilityId()).getCoord());
					}
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
