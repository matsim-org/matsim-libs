/* *********************************************************************** *
 * project: org.matsim.*
 * RenameId.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

public class RenameId {

	public static void main(String [] args) {
		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, "/Users/laemmel/devel/GRIPS/input/config.xml");
		Scenario sc = ScenarioUtils.loadScenario(c);
		Config c2 = ConfigUtils.createConfig();
		Scenario sc2 = ScenarioUtils.createScenario(c2);

		{
			NetworkFactory fac = sc2.getNetwork().getFactory();
			for (Node n : sc.getNetwork().getNodes().values()){
				Node n2 = fac.createNode(Id.create("car"+n.getId().toString(), Node.class), n.getCoord());
				sc2.getNetwork().addNode(n2);
			}
			for (Link l : sc.getNetwork().getLinks().values()) {
				Link l2 = fac.createLink(Id.create("car"+l.getId(), Link.class), Id.create("car"+l.getFromNode().getId().toString(), Node.class), Id.create("car"+l.getToNode().getId().toString(), Node.class));
				sc2.getNetwork().addLink(l2);
				l2.setAllowedModes(l.getAllowedModes());
				l2.setCapacity(l.getCapacity());
				l2.setFreespeed(l.getFreespeed());
				l2.setLength(l.getLength());
				l2.setNumberOfLanes(l.getNumberOfLanes());
			}
			new NetworkWriter(sc2.getNetwork()).write("/Users/laemmel/devel/hhw_hybrid/input/car_network.xml.gz");
		}

		{
			PopulationFactory fac = sc2.getPopulation().getFactory();
			for (Person p : sc.getPopulation().getPersons().values()) {
				Person p2 = fac.createPerson(Id.create("car"+p.getId().toString(), Person.class));
				sc2.getPopulation().addPerson(p2);
				for (Plan pl : p.getPlans()) {
					Plan pl2 = fac.createPlan();
					p2.addPlan(pl2);
					for (PlanElement al : pl.getPlanElements()) {
						if (al instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl)al;
//							ActivityImpl al2 = new ActivityImpl(act.getType(), Id.create("car"+act.getLinkId().toString()));
							Activity al2 = fac.createActivityFromLinkId(act.getType(), Id.create("car"+act.getLinkId().toString(), Link.class));
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
							ids.add(Id.create("car"+r.getStartLinkId().toString(), Link.class));
							for (Id<Link> id : r.getLinkIds()) {
								ids.add(Id.create("car"+id.toString(), Link.class));
							}
//							
							ids.add(Id.create("car"+r.getEndLinkId().toString(), Link.class));
							LinkNetworkRouteImpl r2 = (LinkNetworkRouteImpl) RouteUtils.createNetworkRoute(ids, sc2.getNetwork());
							
							r2.setDistance(r.getDistance());
							r2.setTravelCost(r.getTravelCost());
							r2.setTravelTime(r.getTravelTime());
//							r2.setVehicleId(Id.create("car"+r.getVehicleId().toString()));
							leg2.setRoute(r2);
							
							pl2.addLeg(leg2);
							
						} else {
							throw new RuntimeException("unsupported plan element:" + al);
						}
						
						
					}
				}
			}
			new PopulationWriter(sc2.getPopulation()).write("/Users/laemmel/devel/hhw_hybrid/input/car_plans.xml.gz");
		}


	}
}
