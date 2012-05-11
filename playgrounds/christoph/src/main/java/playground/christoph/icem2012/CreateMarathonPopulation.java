/* *********************************************************************** *
 * project: matsim
 * CreateMarathonPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class CreateMarathonPopulation {
	
	private final int runners = 250;
	
	private final String startEndLink = "106474";
	
	private final String[] trackNodes = new String[]{	
			"2952", "2759", "2951", "2531", "2530", "2529", "4263", "4268", "4468", "3496",
			"2530", "2531", "2951", "4507", "4505", "4504", "4505", "4503", "4506", "4508",
			"2951", "2759", "2758", "2952", "2759", "2951", "2531", "2530", "2529", "2528",
			"2527", "2526", "2525", "2524", "2523", "2522", "2521", "4239", "2522", "2523",
			"2524", "2525", "2526", "2527", "2528", "2529", "2530", "2531", "2951", "2759",
			"2952"};
	
	private NetworkRoute route;
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("/home/cdobler/workspace/matsim/mysimulations/ICEM2012/input/network_ivtch.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		new CreateMarathonPopulation(scenario);
	}
	
	public CreateMarathonPopulation(Scenario scenario) {

		prepareNetwork(scenario);
		
		createRoute(scenario);
		
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		
		Id startEndLinkId = scenario.createId(startEndLink);
		Link startEndLink = scenario.getNetwork().getLinks().get(startEndLinkId);
		
		/*
		 * We create a vector from the end to the start of the link since we
		 * start placing runners at the links end and move to the links start
		 */
		Coord fromCoord = startEndLink.getFromNode().getCoord();
		Coord toCoord = startEndLink.getToNode().getCoord();
		double dx = fromCoord.getX() - toCoord.getX();
		double dy = fromCoord.getY() - toCoord.getY();
		double dxy = CoordUtils.calcDistance(fromCoord, toCoord);
		dx = dx/dxy;
		dy = dy/dxy;
		
		// start 100m from links end
		double xStart = toCoord.getX() + 100*dx;
		double yStart = toCoord.getY() + 100*dy;
		
		// shift start point to the corner of the starter field
		xStart -= 4*dy;
		yStart -= 4*dx;
		
		Random random = MatsimRandom.getLocalInstance();
		int row = 1;
		int column = 0;
		int rowRunners = 8 + random.nextInt(5);	// 8..12 runners per row
		double dColumn = 8. / (rowRunners - 1);	// width of road: 8m
		
		for (int personCount = 0; personCount < runners; personCount++) {
			
			PersonImpl person = (PersonImpl) populationFactory.createPerson(scenario.createId("runner_" + personCount));

			// set random age and gender
			person.setAge((int)(18 + Math.round(random.nextDouble() * 47)));	// 18 .. 65 years old
			if (random.nextDouble() > 0.5) person.setSex("m");
			else person.setSex("f");
			
			Plan plan = populationFactory.createPlan();
			person.addPlan(plan);
			
			ActivityImpl activity;
			
			double x = xStart;
			double y = yStart;
			
			// move to row position
			x += 0.75*row*dx;
			y += 0.75*row*dy;
			
			// move to column position
			x += dColumn*column*dy;
			y += dColumn*column*dx;
			
			// add some random noise (+/- 0.25m in x and y direction)
			x += 0.5*random.nextDouble() - 0.5;
			y += 0.5*random.nextDouble() - 0.5;
			
			column++;
			if (column >= rowRunners) {
				column = 0;
				row++;
				rowRunners = 8 + random.nextInt(5);	// 8..12 runners per row
				dColumn = 8. / (rowRunners - 1);
			}
			
			System.out.println(person.getId().toString() + "\t" + x + "\t" + y);
			
			Coord coord = scenario.createCoord(x, y);
			activity = (ActivityImpl) populationFactory.createActivityFromLinkId("preRun", startEndLinkId);
			activity.setEndTime(9*3600);
			activity.setCoord(coord);
			
			Leg leg = populationFactory.createLeg("walk2d");
			leg.setRoute(route.clone());
			
			activity = (ActivityImpl) populationFactory.createActivityFromLinkId("postRun", startEndLinkId);
			activity.setCoord(coord);
		}
	}
	
	private void createRoute(Scenario scenario) {
		
		List<Id> nodeIds = new ArrayList<Id>();
		for (String nodeId : trackNodes) {
			Id id = scenario.createId(nodeId);
			nodeIds.add(id);
		}
		
		List<Id> linkIds = new ArrayList<Id>();
		for (int i = 0; i < nodeIds.size() - 1; i++) {
			Id fromId = nodeIds.get(i);
			Id toId = nodeIds.get(i + 1);
			
			Node fromNode = scenario.getNetwork().getNodes().get(fromId);
			for (Link link : fromNode.getOutLinks().values()) {
				if (link.getToNode().getId().equals(toId)) {
					linkIds.add(link.getId());
					break;
				}
			}
		}
		
		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();
		Id startLinkId = linkIds.remove(0);
		Id endLinkId = linkIds.remove(linkIds.size() - 1);
		route = (NetworkRoute) routeFactory.createRoute(startLinkId, endLinkId);
		route.setLinkIds(startLinkId, linkIds, endLinkId);
	}
	
	private void prepareNetwork(Scenario scenario) {
		// TODO: add some links, set walk2d instead of car mode for running track
	}
}
