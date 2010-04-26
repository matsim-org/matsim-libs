/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFacility2Link
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.modules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.Desires;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonConsolidateInitDemand extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private static final Logger log = Logger.getLogger(PersonConsolidateInitDemand.class);
	private final Knowledges knowledges;
	private final ActivityFacilities facilities;

	public PersonConsolidateInitDemand(Knowledges knowledges, final ActivityFacilities facilities) {
		super();
		this.knowledges = knowledges;
		this.facilities = facilities;
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	public void run(final Plan p) {
		PlanImpl plan = (PlanImpl) p;
		if (plan.getPerson().getPlans().size() != 1) { throw new RuntimeException("Person id="+plan.getPerson().getId()+" must have exactly one plan."); }

		// get the activity options of the plan
		List<ActivityOption> actOptions = new ArrayList<ActivityOption>();
		ActivityImpl act = plan.getFirstActivity();
		while (act != plan.getLastActivity()) {
			String actType = act.getType();
			ActivityOption ao = this.facilities.getFacilities().get(act.getFacilityId()).getActivityOptions().get(actType);
			if (ao == null) { throw new RuntimeException("Person id="+plan.getPerson().getId()+": act of type="+actType+" does not fit to facility id="+act.getFacilityId()+"!"); }
			actOptions.add(ao);
			act = plan.getNextActivity(plan.getNextLeg(act));
		}
		String actType = act.getType();
		ActivityOption ao = this.facilities.getFacilities().get(act.getFacilityId()).getActivityOptions().get(actType);
		if (ao == null) { throw new RuntimeException("Person id="+plan.getPerson().getId()+": act of type="+actType+" does not fit to facility id="+act.getFacilityId()+"!"); }
		actOptions.add(ao);

		// check desires
		Desires d = plan.getPerson().getDesires();
		if (d == null) { throw new RuntimeException("Person id="+plan.getPerson().getId()+": no desires defined!"); }
		//   check if all acts in the plan are referred by the desires
		for (ActivityOption actOption : actOptions) {
			if (d.getActivityDuration(actOption.getType()) == Time.UNDEFINED_TIME) { throw new RuntimeException("Person id="+plan.getPerson().getId()+": desires of type="+actOption.getType()+" missing!"); }
		}
		//   check and delete acts in desires that are not used
		Set<String> actTypesToDelete = new TreeSet<String>();
		for (String actTypeDes : d.getActivityDurations().keySet()) {
			boolean toDelete = true;
			for (ActivityOption actOption : actOptions) {
				if (actTypeDes.equals(actOption.getType())) { toDelete = false; }
			}
			if (toDelete) { actTypesToDelete.add(actTypeDes); }
		}
		for (String str : actTypesToDelete) {
			log.info("Person id="+plan.getPerson().getId()+": removing act="+str+" of desires.");
			d.getActivityDurations().remove(str);
		}

		// check knowledge
		if (knowledges == null) { throw new RuntimeException("Person id="+plan.getPerson().getId()+": no knowledge defined!"); }
		//   check if all acts in the plan are referred by the knowledge
		for (ActivityOption actOption : actOptions) {
			if (knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities(actOption.getType()).isEmpty()) { throw new RuntimeException("Person id="+plan.getPerson().getId()+": knowledge act of type="+actOption.getType()+" missing!"); }
		}
		//   check and delete acts in knowledge that are not used
		List<ActivityOptionImpl> aoKToDelete = new ArrayList<ActivityOptionImpl>();
		for (ActivityOptionImpl actOptionK : knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities()) {
			boolean toDelete = true;
			for (ActivityOption actOption : actOptions) {
				if (actOptionK.equals(actOption)) { toDelete = false; }
			}
			if (toDelete) { aoKToDelete.add(actOptionK); }
		}
		for (ActivityOptionImpl aok : aoKToDelete) {
			log.info("Person id="+plan.getPerson().getId()+": removing act="+aok.getType()+" of knowledge.");
			if (!knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).removeActivity(aok)) { throw new RuntimeException("Person id="+plan.getPerson().getId()+": could not remove act="+aok.getType()+" from knowledge!"); }
		}

		// doublecheck everything again...
		List<ActivityOptionImpl> kActOptions = knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId()).getActivities();
		if (!kActOptions.containsAll(actOptions) || !actOptions.containsAll(kActOptions)) {
			throw new RuntimeException("Person id="+plan.getPerson().getId()+": plan<=>know does not fit!");
		}
		Set<String> dActTypes = plan.getPerson().getDesires().getActivityDurations().keySet();
		Set<String> pActTypes = new HashSet<String>();
		for (ActivityOption aop : actOptions) { pActTypes.add(aop.getType()); }
		if (!dActTypes.equals(pActTypes)) {
			throw new RuntimeException("Person id="+plan.getPerson().getId()+": plan<=>des does not fit!");
		}
		Set<String> kActTypes = new TreeSet<String>();
		for (ActivityOption aok : kActOptions) { kActTypes.add(aok.getType()); }
		if (!dActTypes.equals(kActTypes)) {
			throw new RuntimeException("Person id="+plan.getPerson().getId()+": know<=>des does not fit!");
		}
	}
}
