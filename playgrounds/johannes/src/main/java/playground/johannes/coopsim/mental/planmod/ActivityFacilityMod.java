/* *********************************************************************** *
 * project: org.matsim.*
 * AcitivityFacilityMod.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.mental.planmod;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.utils.LegRouter;

/**
 * @author illenberger
 * 
 */
public class ActivityFacilityMod implements PlanModifier {

	private final PopulationFactory factory;

	private final LegRouter router;
	
	private final ActivityFacilities facilities;
	
	private int planIndex;

	private ActivityFacility facility;
	
//	private long routingTime;
	
//	private int count;

	public ActivityFacilityMod(ActivityFacilities facilities, LegRouter router) {
		this.facilities = facilities;
		this.router = router;
        this.factory = (PopulationFactory) ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
	}
	
	public void setPlanIndex(int planIndex) {
		this.planIndex = planIndex;
	}

	public void setFacilityId(Id facilityId) {
		this.facility = facilities.getFacilities().get(facilityId);
	}

	@Override
	public void apply(Plan plan) {
		Activity act = (Activity) plan.getPlanElements().get(planIndex);

		Activity newAct = factory.createActivityFromLinkId(act.getType(), facility.getLinkId());
		((Activity) newAct).setFacilityId(facility.getId());

		newAct.setEndTime(act.getEndTime());

		plan.getPlanElements().set(planIndex, newAct);

		if (planIndex > 1) {
			Activity prev = (Activity) plan.getPlanElements().get(planIndex - 2);
			Leg toLeg = (Leg) plan.getPlanElements().get(planIndex - 1);
//			long time = System.currentTimeMillis();
			router.routeLeg(plan.getPerson(), toLeg, prev, newAct, prev.getEndTime());
//			routingTime += System.currentTimeMillis() - time;
		}

		if (planIndex < plan.getPlanElements().size() - 2) {
			Leg fromLeg = (Leg) plan.getPlanElements().get(planIndex + 1);
			Activity next = (Activity) plan.getPlanElements().get(planIndex + 2);
//			long time = System.currentTimeMillis();
			router.routeLeg(plan.getPerson(), fromLeg, newAct, next, newAct.getEndTime());
//			routingTime += System.currentTimeMillis() - time;
		}
		
//		count++;
//		if(count % 500 == 0) {
//			System.out.println(String.format("routing time = %1$s", routingTime));
//			routingTime = 0;
//		}
	}

}
