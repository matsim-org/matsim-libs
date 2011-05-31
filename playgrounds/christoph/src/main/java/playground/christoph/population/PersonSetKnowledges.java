/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetFacilities.java
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

package playground.christoph.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonSetKnowledges extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(UpdateCrossboarderPopulation.class);
	
	private static final String HOME = "home";
	private static final String WORK_SECTOR2 = "work_sector2";
	private static final String WORK_SECTOR3 = "work_sector3";
	private static final String TTA = "tta";
	
	private ActivityFacilities activityFacilities;
	private Knowledges knowledges;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSetKnowledges(Scenario scenario) {
		super();
		log.info("init " + this.getClass().getName() + " module...");
		this.activityFacilities = ((ScenarioImpl)scenario).getActivityFacilities();
		this.knowledges = ((ScenarioImpl)scenario).getKnowledges();
		log.info("done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		run(person.getSelectedPlan());
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public void run(Plan plan) {
		KnowledgeImpl knowledge = knowledges.getFactory().createKnowledge(plan.getPerson().getId(), "");
		
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				
				Facility facility = activityFacilities.getFacilities().get(activity.getFacilityId());
				
				ActivityOption activityOption = ((ActivityFacility) facility).getActivityOptions().get(activity.getType());
				
				// By default an activity is not primary. If it is home, work or tta, we set it to primary.
				boolean isPrimary = false;
				if (activity.getType().equals(HOME)) isPrimary = true;
				else if (activity.getType().equals(WORK_SECTOR2)) isPrimary = true;
				else if (activity.getType().equals(WORK_SECTOR3)) isPrimary = true;
				else if (activity.getType().equals(TTA)) isPrimary = true;

				knowledge.addActivityOption((ActivityOptionImpl)activityOption, isPrimary);
			}
		}
		int i = 0;
		i++;
	}
}
