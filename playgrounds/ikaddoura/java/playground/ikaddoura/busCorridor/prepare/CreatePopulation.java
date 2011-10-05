/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePopulation.java
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

package playground.ikaddoura.busCorridor.prepare;


import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class CreatePopulation implements Runnable {
	private Map<String, Coord> zoneGeometries = new HashMap<String, Coord>();
	private Scenario scenario;
	private Population population;
		
	public static void main(String[] args) {
		CreatePopulation potsdamPopulation = new CreatePopulation();
		potsdamPopulation.run();
		
	}

	public void run(){
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		
		fillZoneData();
		
		generatePopulation();
		
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("../../shared-svn/studies/ihab/busCorridor/input/population.xml");
	}

	private void fillZoneData() {
		zoneGeometries.put("node1", scenario.createCoord(1900, 0));
		zoneGeometries.put("node2", scenario.createCoord(2000, 0));
		zoneGeometries.put("node3", scenario.createCoord(4000, 0));
		zoneGeometries.put("node4", scenario.createCoord(6000, 0));
		zoneGeometries.put("node5", scenario.createCoord(8000, 0));
		zoneGeometries.put("node6", scenario.createCoord(10000, 0));
		zoneGeometries.put("node7", scenario.createCoord(12000, 0));
		zoneGeometries.put("node8", scenario.createCoord(14000, 0));
		zoneGeometries.put("node9", scenario.createCoord(16000, 0));
		zoneGeometries.put("node10", scenario.createCoord(18000, 0));
		zoneGeometries.put("node11", scenario.createCoord(18100, 0));

	}
	
	private void generatePopulation() {
		generateHomeWorkHomeTripsPt("node2", "node10", 30); // home, work, anzahl
		generateHomeWorkHomeTripsPt("node10", "node2", 30); // home, work, anzahl
		generateHomeWorkHomeTripsPt("node3", "node5", 20); // home, work, anzahl
		generateHomeWorkHomeTripsPt("node5", "node3", 20); // home, work, anzahl
		generateHomeWorkShoppingHomeTripsPt("node3", "node9", "node5", 20); // home, work, shopping, anzahl
		generateHomeWorkShoppingHomeTripsPt("node9", "node3", "node6", 20); // home, work, shopping, anzahl

	}

	private void generateHomeWorkHomeTripsPt(String zone1, String zone2, int quantity) {
		for (int i=0; i<quantity; ++i) {
			
			Coord homeLocation = blur(zone1);
			Coord workLocation = blur(zone2);
			
			Person person = population.getFactory().createPerson(createId(zone1, zone2, i, TransportMode.pt));
			Plan plan = population.getFactory().createPlan();
			
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLegPt());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLegPt());
			Activity homeActivity1 = (Activity) plan.getPlanElements().get(0);
			double homeEndTime = homeActivity1.getEndTime();
			Activity homeActivity2 = homeActivity1;
			homeActivity2.setEndTime(homeEndTime);
			plan.addActivity(homeActivity2);
			person.addPlan(plan);
			population.addPerson(person);
		}
	}
	
	private void generateHomeWorkShoppingHomeTripsPt(String zone1, String zone2, String zone3, int quantity) {
		for (int i=0; i<quantity; ++i) {
			
			Coord homeLocation = blur(zone1);
			Coord workLocation = blur(zone2);
			Coord shoppingLocation = blur(zone3);

			Person person = population.getFactory().createPerson(createId(zone1, zone2, zone3, i, TransportMode.pt));
			Plan plan = population.getFactory().createPlan();
			
			plan.addActivity(createHome(homeLocation));
			plan.addLeg(createDriveLegPt());
			plan.addActivity(createWork(workLocation));
			plan.addLeg(createDriveLegPt());
			plan.addActivity(createShopping(plan, shoppingLocation));
			plan.addLeg(createDriveLegPt());
			
			Activity homeActivity1 = (Activity) plan.getPlanElements().get(0);
			double homeEndTime = homeActivity1.getEndTime();
			Activity homeActivity2 = homeActivity1;
			homeActivity2.setEndTime(homeEndTime);
			plan.addActivity(homeActivity2);
			
			person.addPlan(plan);
			population.addPerson(person);
		}
	}
		
	private Coord blur(String zone) {
		Random rnd = new Random();
		double xCoord = zoneGeometries.get(zone).getX()+rnd.nextDouble()*50-rnd.nextDouble()*50;
		double yCoord = zoneGeometries.get(zone).getY()+rnd.nextDouble()*50-rnd.nextDouble()*50;
		Coord zoneCoord = scenario.createCoord(xCoord, yCoord);
		return zoneCoord;
	}
	
	private Leg createDriveLegPt() {
		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		return leg;
	}

	private Activity createWork(Coord workLocation) {
		Random rnd = new Random();
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime((16*60*60)+(rnd.nextDouble()*60*60)-(rnd.nextDouble()*60*60));
		return activity;
	}
	
	private Activity createShopping(Plan plan, Coord shoppingLocation) {
		Random rnd = new Random();
		Activity activity = population.getFactory().createActivityFromCoord("shopping", shoppingLocation);
		Activity work = (Activity) plan.getPlanElements().get(2);
		double endWorkTime = work.getEndTime();
		double minimalShoppingTime = 30;
		activity.setEndTime(endWorkTime+minimalShoppingTime+(rnd.nextDouble()*60*60));
		return activity;
	}

	private Activity createHome(Coord homeLocation) {
		Random rnd = new Random();
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(8*60*60+(rnd.nextDouble()*60*60)-(rnd.nextDouble()*60*60));
		return activity;
	}

	private Id createId(String zone1, String zone2, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + zone1 + "_" + zone2 + "_" + i);
	}
	
	private Id createId(String zone1, String zone2, String zone3, int i, String transportMode) {
		return new IdImpl(transportMode + "_" + zone1 + "_" + zone2 + "_" + zone3 + "_" + i);
	}
}

