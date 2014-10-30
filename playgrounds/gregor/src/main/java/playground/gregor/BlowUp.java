/* *********************************************************************** *
 * project: org.matsim.*
 * BlowUp.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.gregor;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class BlowUp {
	public static void main(String [] args) {
		String conf = "/Users/laemmel/devel/GRIPS/input/config.xml";
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, conf);
		Scenario sc = ScenarioUtils.loadScenario(c);

		int idc = 0;
		List<Person> blowups = new ArrayList<Person>();
		PopulationFactory fac = sc.getPopulation().getFactory();
		for (Person p : sc.getPopulation().getPersons().values()) {
			for (int i = 0; i < 9; i++) {
				Person p2 = fac.createPerson(Id.create("blowup"+(idc++), Person.class));
				blowups.add(p2);
				for (Plan pl : p.getPlans()) {
					Plan pl2 = fac.createPlan();
					p2.addPlan(pl2);
					for (PlanElement al : pl.getPlanElements()) {
						if (al instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl)al;
							//						ActivityImpl al2 = new ActivityImpl(act.getType(), Id.create("car"+act.getLinkId().toString()));
							Activity al2 = fac.createActivityFromLinkId(act.getType(), Id.create(act.getLinkId().toString(), Link.class));
							al2.setEndTime(act.getEndTime());
							al2.setMaximumDuration(act.getMaximumDuration());
							al2.setStartTime(act.getStartTime());
							pl2.addActivity(al2);
						} else if (al instanceof LegImpl){
							LegImpl leg = (LegImpl)al;
							Leg leg2 = fac.createLeg(leg.getMode());
							leg2.setDepartureTime(leg.getDepartureTime());
							leg2.setTravelTime(leg.getTravelTime());
							LinkNetworkRouteImpl r = (LinkNetworkRouteImpl) leg.getRoute();
							List<Id<Link>>ids = new ArrayList<Id<Link>>();
							ids.add(Id.create(r.getStartLinkId().toString(), Link.class));
							for (Id<Link> id : r.getLinkIds()) {
								ids.add(Id.create(id.toString(), Link.class));
							}
							//						
							ids.add(Id.create(r.getEndLinkId().toString(), Link.class));
							LinkNetworkRouteImpl r2 = (LinkNetworkRouteImpl) RouteUtils.createNetworkRoute(ids, sc.getNetwork());

							r2.setDistance(r.getDistance());
							r2.setTravelCost(r.getTravelCost());
							r2.setTravelTime(r.getTravelTime());
							//						r2.setVehicleId(Id.create("car"+r.getVehicleId().toString()));
							leg2.setRoute(r2);

							pl2.addLeg(leg2);

						} else {
							throw new RuntimeException("unsupported plan element:" + al);
						}


					}
				}
			}
		}

		for (Person p : blowups) {
			sc.getPopulation().addPerson(p);
		}
		new PopulationWriter(sc.getPopulation()).write("/Users/laemmel/devel/GRIPS/input/blow.xml.gz");
	}
}
