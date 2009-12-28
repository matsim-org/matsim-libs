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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.MicroCensus;

public class PersonAssignActivityChains extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignActivityChains.class);

	private final MicroCensus microcensus;

	private Knowledges knowledges;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignActivityChains(MicroCensus microcensus, Knowledges knowledges) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.microcensus = microcensus;
		this.knowledges = knowledges;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person p) {
		PersonImpl person = (PersonImpl) p;
		boolean has_work = false;
		if (!this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_W2).isEmpty() ||
		    !this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_W3).isEmpty()) {
			has_work = true;
		}
		boolean has_educ = false;
		if (!this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EKIGA).isEmpty() ||
		    !this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EPRIM).isEmpty() ||
		    !this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_ESECO).isEmpty() ||
		    !this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EHIGH).isEmpty() ||
		    !this.knowledges.getKnowledgesByPersonId().get(person.getId()).getActivities(CAtts.ACT_EOTHR).isEmpty()) {
			has_educ = true;
		}

		Person mz_p = microcensus.getRandomWeightedMZPerson(person.getAge(),person.getSex(),person.getLicense(), has_work, has_educ);
		if (mz_p == null) {
			log.warn("pid="+person.getId()+": Person does not belong to a micro census group!");
			mz_p = microcensus.getRandomWeightedMZPerson(person.getAge(),"f",person.getLicense(), has_work, has_educ);
			log.warn("=> Assigning same demographics except that person is handled as a female. NOTE: Works only for CH-Microcensus 2005.");
			if (mz_p == null) {
				Gbl.errorMsg("In CH-Microcensus 2005: That should not happen!");
			}
		}
		person.addPlan(mz_p.getSelectedPlan());
		person.setSelectedPlan(mz_p.getSelectedPlan());
	}

	public void run(Plan plan) {
	}
}
