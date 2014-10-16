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

package playground.ikaddoura.parkAndRide.prepare;


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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CreatePopulationTestScenario implements Runnable {

	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/testScenario_network.xml";
//	private Map<String, Coord> zoneGeometries = new HashMap<String, Coord>();
	private Scenario scenario;
	private Population population;

	public static void main(String[] args) {
		CreatePopulationTestScenario populationCreator = new CreatePopulationTestScenario();
		populationCreator.run();
	}

	@Override
	public void run(){
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.population = this.scenario.getPopulation();
		Config config = this.scenario.getConfig();
		config.network().setInputFile(networkFile);
		ScenarioUtils.loadScenario(this.scenario);
		
//		fillZoneData();
		generatePopulation();
		
		PopulationWriter populationWriter = new PopulationWriter(this.population, scenario.getNetwork());
		populationWriter.write("../../shared-svn/studies/ihab/parkAndRide/input/testScenario_population.xml");
	}

//	private void fillZoneData() {
//		for (Node node : this.scenario.getNetwork().getNodes().values()){
//			this.zoneGeometries.put(node.getId().toString(), node.getCoord());
//		}
//	}
//	
	private void generatePopulation() {
		createWorkTrips(300);
//		createOtherTrips(2600);
	}
	
	private void createWorkTrips(int quantity) {
		for (int i=0; i<quantity; i++){

			Coord homeLocation = scenario.createCoord(-100.0, 0);		
			Coord workLocation = scenario.createCoord(10100.0, 0);
			
			double homeEndTimeRnd = calculateNormallyDistributedTime(7.0*3600.0, 3600.0);

//			if (i <= ((double)quantity / 2.0)){
//				Person person = this.population.getFactory().createPerson(createId("person_availCar_Pt_", String.valueOf((int)homeLocation.getX()), String.valueOf((int)workLocation.getX()), i));
//				Plan plan = this.population.getFactory().createPlan();
//	
//				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
//				plan.addLeg(createDriveLegPt());
//				plan.addActivity(createWork(workLocation, homeEndTimeRnd + (9*60*60)));
//				plan.addLeg(createDriveLegPt());
//				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
//				person.addPlan(plan);
//				this.population.addPerson(person);
//			}
			
//			if (i > ((double)quantity / 2.0)){
				Person person = this.population.getFactory().createPerson(createId("person_car_", String.valueOf((int)homeLocation.getX()), String.valueOf((int)workLocation.getX()), i));
				Plan plan = this.population.getFactory().createPlan();
	
				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
				plan.addLeg(createDriveLegCar());
				plan.addActivity(createWork(workLocation, homeEndTimeRnd + (9*60*60)));
				plan.addLeg(createDriveLegCar());
				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
				person.addPlan(plan);
				this.population.addPerson(person);
//			}
		}
	}

//	private void createOtherTrips(int quantity) {
//		for (int i=0; i<quantity; i++){
//			Coord homeLocation = getRndCoord();
//			Coord otherLocation = getRndCoord();
//			double homeEndTimeRnd = calculateRandomlyDistributedValue(12.5 * 60*60, 4.5*60*60); // 8 - 17 Uhr
//			
//			if (i <= ((double)quantity / 2.0)){
//				Person person = this.population.getFactory().createPerson(createId("person_HomeOtherHomePt_", String.valueOf((int)homeLocation.getX()), String.valueOf((int)otherLocation.getX()), i));
//				Plan plan = this.population.getFactory().createPlan();
//	
//				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
//				plan.addLeg(createDriveLegPt());
//				plan.addActivity(createOther(otherLocation, homeEndTimeRnd + (2*60*60)));
//				plan.addLeg(createDriveLegPt());
//				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
//				person.addPlan(plan);
//				this.population.addPerson(person);
//			}
//			
//			if (i > ((double)quantity / 2.0)){
//				Person person = this.population.getFactory().createPerson(createId("person_HomeOtherHomeCar_", String.valueOf((int)homeLocation.getX()), String.valueOf((int)otherLocation.getX()), i));
//				Plan plan = this.population.getFactory().createPlan();
//	
//				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
//				plan.addLeg(createDriveLegCar());
//				plan.addActivity(createOther(otherLocation, homeEndTimeRnd + (2*60*60)));
//				plan.addLeg(createDriveLegCar());
//				plan.addActivity(createHome(homeLocation, homeEndTimeRnd));
//				person.addPlan(plan);
//				this.population.addPerson(person);
//			}
//		}
//	}
	
	private double calculateNormallyDistributedTime(double i, double abweichung) {
		Random random = new Random();
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		//linear transformation in order to optain N[i,7200²]
		double endTimeInSec = i + abweichung * normal ;
		return endTimeInSec;
	}
	
//	private double calculateRandomlyDistributedValue(double i, double abweichung){
//		Random random = new Random();
//		double rnd1 = random.nextDouble();
//		double rnd2 = random.nextDouble();
//		double vorzeichen = 0;
//		if (rnd1<=0.5){
//			vorzeichen = -1.0;
//		}
//		else {
//			vorzeichen = 1.0;
//		}
//		double endTimeInSec = (i + (rnd2 * abweichung * vorzeichen));
//		return endTimeInSec;
//	}
	
//	private Coord getRndCoord() {
//		double minXCoord = this.zoneGeometries.get("0").getX();
//		double maxXCoord = this.zoneGeometries.get(String.valueOf(this.zoneGeometries.size()-1)).getX();
//
//		for (String zone: this.zoneGeometries.keySet()){
//			double xCoord = this.zoneGeometries.get(zone).getX();
//			
//			if (xCoord < minXCoord){
//				minXCoord = xCoord;
//			}
//			if (xCoord > maxXCoord){
//				maxXCoord = xCoord;
//			}
//		}
//		
//		double area = maxXCoord - minXCoord;
//		double randomXCoord = calculateRandomlyDistributedValue((area/2.0), (area/2.0));
//		Coord zoneCoord = this.scenario.createCoord(randomXCoord, 0);
//		return zoneCoord;
//	}
	
	private Leg createDriveLegPt() {
		Leg leg = this.population.getFactory().createLeg(TransportMode.pt);
		return leg;
	}
	
	private Leg createDriveLegCar() {
		Leg leg = this.population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private Activity createOther(Coord otherLocation, double endTime) {
		Activity activity = this.population.getFactory().createActivityFromCoord("other", otherLocation);
		activity.setEndTime(endTime);
		return activity;
	}
	
	private Activity createWork(Coord workLocation, double endTime) {
		Activity activity = this.population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(endTime);
		return activity;
	}

	private Activity createHome(Coord homeLocation, double endTime) {
		Activity activity = this.population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(endTime);
		return activity;
	}

	private Id<Person> createId(String name, String zone1, String zone2, int i) {
		return Id.create(name + zone1 + "_" + zone2 + "_" + i, Person.class);
	}
	
}

