/* *********************************************************************** *
 * project: org.matsim.*												   *
 * OnlyTimeDependentTravelCostCalculator.java							   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.dziemke.other;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 */
class CreatePopulation {

	public static void main(String[] args) {
		final int numberOfAgents = 1000;
		final int numberOfPlans = 9;
		boolean includeRoute = true;
		final String networkFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/network.xml";
		final String plansFile = "../../../shared-svn/projects/cemdapMatsimCadyts/cadyts/equil/input/plans" + numberOfAgents + "_" + numberOfPlans + ".xml";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation();   
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		Network network = scenario.getNetwork();
		
		for (int i = 1; i <= numberOfAgents; i++) {
			Person person = population.getFactory().createPerson(Id.create(i, Person.class));
			for (int j = 1; j <= numberOfPlans; j++) {
			Plan plan = population.getFactory().createPlan();
			{
				Activity activity = population.getFactory().createActivityFromLinkId("h", Id.createLinkId("1"));
				activity.setEndTime(6*60*60);
				plan.addActivity(activity);
			}
			{
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				if (includeRoute == true) {
					List<Id<Link>> routeLinkIds = new ArrayList<>();
					routeLinkIds.add(Id.createLinkId("1"));
					// The following two links are varied among the plans to create route diversity
					routeLinkIds.add(Id.createLinkId(1 + j));
					routeLinkIds.add(Id.createLinkId(10 +j));
					routeLinkIds.add(Id.createLinkId("20"));
					Route route = RouteUtils.createNetworkRoute(routeLinkIds, network);
					leg.setRoute(route);
				}
				plan.addLeg(leg);
			}
			{
				Activity activity = population.getFactory().createActivityFromLinkId("w", Id.createLinkId("20"));
				activity.setMaximumDuration(30*60);
				plan.addActivity(activity);
			}
			{
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				if (includeRoute == true) {
					List<Id<Link>> routeLinkIds = new ArrayList<>();
					routeLinkIds.add(Id.createLinkId("20"));
					routeLinkIds.add(Id.createLinkId("21"));
					routeLinkIds.add(Id.createLinkId("22"));
					routeLinkIds.add(Id.createLinkId("23"));
					routeLinkIds.add(Id.createLinkId("1"));
					Route route = RouteUtils.createNetworkRoute(routeLinkIds, network);
					leg.setRoute(route);
				}
				plan.addLeg(leg);
			}
			{
				Activity activity = population.getFactory().createActivityFromLinkId("h", Id.createLinkId("1"));
				plan.addActivity(activity);
			}
			person.addPlan(plan);
			}
			population.addPerson(person);
		}
		MatsimWriter popWriter = new PopulationWriter(population, network);
		popWriter.write(plansFile);
	}
}
