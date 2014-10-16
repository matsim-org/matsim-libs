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
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePopulationWorkOther implements Runnable {
	private Map<String, Coord> zoneGeometries = new HashMap<String, Coord>();
	private Scenario scenario;
	private Population population;
	private String networkFile = "../../shared-svn/studies/ihab/busCorridor/input_final_2/network80links.xml";

		
	public static void main(String[] args) {
		CreatePopulationWorkOther potsdamPopulation = new CreatePopulationWorkOther();
		potsdamPopulation.run();
		
	}

	@Override
	public void run(){
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		population = scenario.getPopulation();
		
		fillZoneData();
		
		generatePopulation();
		
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write("../../shared-svn/studies/ihab/busCorridor/input_final_2/populationBusCorridor80linksCar.xml");
	}

	private void fillZoneData() {
		Config config = scenario.getConfig();
		config.network().setInputFile(this.networkFile);
		ScenarioUtils.loadScenario(scenario);
		for (Node node : scenario.getNetwork().getNodes().values()){
			zoneGeometries.put(node.getId().toString(), node.getCoord());
		}
	}
	
	private double calculateNormallyDistributedTime(double i, double abweichung) {
		Random random = new Random();
		
		//draw two random numbers [0;1] from uniform distribution
//		double r1 = random.nextDouble();
//		double r2 = random.nextDouble();
//		//Box-Muller-Method in order to get a normally distributed variable
//		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
//		//linear transformation in order to optain N[i,st.deviation²]
//		double endTimeInSec = i + abweichung * normal ;

		double normal = random.nextGaussian();
		double endTimeInSec = i + abweichung * normal;
		// not yet tested
		
		return endTimeInSec;
	}
	
	private double calculateRandomlyDistributedValue(double i, double abweichung){
		Random random = new Random();
		double rnd1 = random.nextDouble();
		double rnd2 = random.nextDouble();
		double vorzeichen = 0;
		if (rnd1<=0.5){
			vorzeichen = -1.0;
		}
		else {
			vorzeichen = 1.0;
		}
		double endTimeInSec = (i + (rnd2 * abweichung * vorzeichen));
		return endTimeInSec;
	}
	
	private void generatePopulation() {
		
		createOtherTrips(2600);
		createWorkTrips(1400);

	}
	
	private void createWorkTrips(int quantity) {
		for (int i=0; i<quantity; i++){
			Coord homeLocation = getRndCoord();
//			Coord homeLocation = new CoordImpl(0, 0);
			
			Coord workLocation = getRndCoord();
//			Coord workLocation = new CoordImpl(5000, 0);

			
//			double homeEndTimeRnd = calculateNormallyDistributedTime(8*60*60, 1*60*60);
//			double homeEndTimeRnd = calculateRandomlyDistributedValue(8*3600, 1.5*3600);
			double homeEndTimeRnd = 8*3600;

			Person person = population.getFactory().createPerson(createId("person_HomeWorkHome_", String.valueOf((int)homeLocation.getX()), String.valueOf((int)workLocation.getX()), i));
			Plan plan = population.getFactory().createPlan();

			plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
			plan.addLeg(createDriveLegPt());
			plan.addActivity(createWork(workLocation, homeEndTimeRnd + (8*60*60)));
			plan.addLeg(createDriveLegPt());
			plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
			person.addPlan(plan);
			population.addPerson(person);

		}
	}

	private void createOtherTrips(int quantity) {
		for (int i=0; i<quantity; i++){
			Coord homeLocation = getRndCoord();
			Coord otherLocation = getRndCoord();
			double homeEndTimeRnd = calculateRandomlyDistributedValue(12.5 * 60*60, 4.5*60*60); // 8 - 17 Uhr
			
			Person person = population.getFactory().createPerson(createId("person_HomeOtherHome_", String.valueOf((int)homeLocation.getX()), String.valueOf((int)otherLocation.getX()), i));
			Plan plan = population.getFactory().createPlan();

			plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
			plan.addLeg(createDriveLegPt());
			plan.addActivity(createOther(otherLocation, homeEndTimeRnd + (2*60*60)));
			plan.addLeg(createDriveLegPt());
			plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
			person.addPlan(plan);
			population.addPerson(person);

		}
		
	}
	
	private Coord getRndCoord() {
		double minXCoord = zoneGeometries.get("0").getX();
		double maxXCoord = zoneGeometries.get(String.valueOf(zoneGeometries.size()-1)).getX();

		for (String zone: zoneGeometries.keySet()){
			double xCoord = zoneGeometries.get(zone).getX();
			
			if (xCoord < minXCoord){
				minXCoord = xCoord;
			}
			if (xCoord > maxXCoord){
				maxXCoord = xCoord;
			}
		}
		
		double area = maxXCoord - minXCoord;
		double randomXCoord = calculateRandomlyDistributedValue((area/2.0), (area/2.0));
		Coord zoneCoord = scenario.createCoord(randomXCoord, 0);
		return zoneCoord;
	}
	
	private Leg createDriveLegPt() {
//		Leg leg = population.getFactory().createLeg(TransportMode.pt);
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Activity createOther(Coord otherLocation, double endTime) {
		Activity activity = population.getFactory().createActivityFromCoord("other", otherLocation);
		activity.setEndTime(endTime);
		return activity;
	}
	
	private Activity createWork(Coord workLocation, double endTime) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(endTime);
		return activity;
	}

	private Activity createHome(Coord homeLocation, double endTime) {
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(endTime);
		return activity;
	}

	private Id<Person> createId(String name, String zone1, String zone2, int i) {
		return Id.create(name + zone1 + "_" + zone2 + "_" + i, Person.class);
	}
	
}

