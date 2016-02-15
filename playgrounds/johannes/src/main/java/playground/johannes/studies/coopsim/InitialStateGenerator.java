/* *********************************************************************** *
 * project: org.matsim.*
 * RandomStateGenerator.java
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
package playground.johannes.studies.coopsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialVertex;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.johannes.coopsim.utils.NetworkLegRouter;

/**
 * @author illenberger
 *
 */
public class InitialStateGenerator {

	public static void generate(SocialGraph graph, ActivityFacilities facilities, NetworkLegRouter router) {
        PopulationFactory factory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
		/*
		 * delete all plans
		 */
		for(SocialVertex v : graph.getVertices()) {
			v.getPerson().getPerson().getPlans().clear();
		}
		/*
		 * create new plans
		 */
		for(SocialVertex v : graph.getVertices()) {
			Person person = v.getPerson().getPerson();
			
			ActivityFacility homeFac = facilities.getFacilities().get(Id.create(FacilityValidator.HOME_PREFIX + person.getId().toString(), ActivityFacility.class)); 
			
			Plan plan = factory.createPlan();
			
			Activity home1 = factory.createActivityFromLinkId("home", homeFac.getLinkId());
			((ActivityImpl)home1).setFacilityId(homeFac.getId());
			home1.setEndTime(8*60*60);
			
			Activity home2 = factory.createActivityFromLinkId("idle", homeFac.getLinkId());
			((ActivityImpl)home2).setFacilityId(homeFac.getId());
			home2.setEndTime(16*60*60);
			
			Activity home3 = factory.createActivityFromLinkId("home", homeFac.getLinkId());
			((ActivityImpl)home3).setFacilityId(homeFac.getId());
			home3.setEndTime(24*60*60);
			
			Leg leg1 = factory.createLeg("car");
			router.routeLeg(person, leg1, home1, home2, home1.getEndTime());
			
			Leg leg2 = factory.createLeg("car");
			router.routeLeg(person, leg2, home2, home3, home2.getEndTime());
			
			plan.addActivity(home1);
			plan.addLeg(leg1);
			plan.addActivity(home2);
			plan.addLeg(leg2);
			plan.addActivity(home3);
			
			person.addPlan(plan);
			person.setSelectedPlan(plan);
		}
	}
}
