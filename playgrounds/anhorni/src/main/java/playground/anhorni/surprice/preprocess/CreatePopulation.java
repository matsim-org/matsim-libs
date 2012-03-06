/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice.preprocess;

import java.io.File;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.surprice.AgentMemories;
import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.DecisionModel;
import playground.anhorni.surprice.DecisionModels;

public class CreatePopulation {
	private ScenarioImpl scenario = null;	
	private Config config;
	private final static Logger log = Logger.getLogger(CreatePopulation.class);	
	private DecisionModels decisionModels = new DecisionModels();
	private AgentMemories memories = new AgentMemories();
	
	private TreeMap<Id, ActivityFacility> homeLocations = new TreeMap<Id, ActivityFacility>();
	private TreeMap<Id, ActivityFacility> workLocations = new TreeMap<Id, ActivityFacility>();
	
	private DecisionModelCreator decisionModelCreator = new DecisionModelCreator();
	private Random random = new Random(37835409);
	
	private Zone tollZone;
			
	public void createPopulation(ScenarioImpl scenario, Config config) {		
		this.scenario = scenario;
		this.config = config;		
		this.createPersons();
				
		for (String day : CreateScenario.days) {
			for (Person p : this.scenario.getPopulation().getPersons().values()) {	
				DecisionModel decisionModel = this.decisionModelCreator.createDecisionModelForAgent((PersonImpl)p, this.memories.getMemory(p.getId()));
				this.decisionModels.addDecsionModelForAgent(decisionModel, p.getId());
				this.createDemandForPerson((PersonImpl)p, day);	
			}
		}
		for (String day : CreateScenario.days) {
			// choose random plan for day
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				// set plan and remove it from memory
				Plan plan = this.memories.getMemory(person.getId()).getRandomPlanAndRemove(day, this.random);
				((PersonImpl)person).addPlan(plan);
				((PersonImpl)person).setSelectedPlan(plan);
			}
			String outPath = config.findParam(CreateScenario.SURPRICE, "outPath") + day;
			new File(outPath).mkdirs();
			this.write(outPath);
			
			// clear plan for day from persons
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				((PersonImpl)person).getPlans().clear();
			}
		}
	}
	
	private void initZone(Zone zone) {
		// fill zones
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {	
			if (zone.inZone(facility.getCoord())) {
				zone.addFacility(facility);
			}
		}
	}
	
	private void createPersons() {
		double sideLength = Double.parseDouble(config.findParam(CreateScenario.SURPRICE, "sideLength"));
		Zone centerZone = new Zone("centerZone", (Coord) new CoordImpl(sideLength / 2.0 - 500.0, sideLength / 2.0 + 500.0), 1000.0, 1000.0);	
		this.initZone(centerZone);
		
		this.tollZone = centerZone;
		
		Zone topLeftZone =  new Zone("bottomLeftZone", (Coord) new CoordImpl(0.0, sideLength), 1000.0, 1000.0); 
		this.initZone(topLeftZone);
		Zone bottomLeftZone =  new Zone("bottomLeftZone", (Coord) new CoordImpl(0.0, 1000.0), 1000.0, 1000.0); 
		this.initZone(bottomLeftZone);
		Zone bottomRightZone =  new Zone("bottomRightZone", (Coord) new CoordImpl(sideLength - 1000.0, 0.0), 1000.0, 1000.0);
		this.initZone(bottomRightZone);		
		Zone topRightLargeZone = new Zone("topRightLargeZone", (Coord) new CoordImpl(sideLength - 2000.0, sideLength), 2000.0, 2000.0);
		this.initZone(topRightLargeZone);
		Zone bottomRightLargeZone = new Zone("bottomRightLargeZone", (Coord) new CoordImpl(sideLength - 2000.0, 2000.0), 2000.0, 2000.0);
		this.initZone(bottomRightLargeZone);
		
		int personCnt = this.addPersons(centerZone, centerZone, 0);
		personCnt += this.addPersons(topLeftZone, centerZone, personCnt);
		personCnt += this.addPersons(bottomLeftZone, topRightLargeZone, personCnt);
		personCnt += this.addPersons(bottomLeftZone, topRightLargeZone, personCnt);
		personCnt += this.addPersons(bottomRightZone, bottomRightLargeZone, personCnt);
		
		log.info("Created " + personCnt + " persons");
	}
			
	private int addPersons(Zone origin, Zone destination, int offset) {				
		int personsPerLocation = Integer.parseInt(config.findParam(CreateScenario.SURPRICE, "personsPerZone"));
		int personCnt = 0;
		for (int j = 0; j < personsPerLocation; j++) {
			PersonImpl p = new PersonImpl(new IdImpl(personCnt + offset));			
			personCnt++;
			this.scenario.getPopulation().addPerson(p);	
			ActivityFacility home = origin.getRandomLocationInZone(this.random);
			this.homeLocations.put(p.getId(), home);
						
			ActivityFacility work = destination.getRandomLocationInZone(this.random);
			this.workLocations.put(p.getId(), work);
			
			this.memories.addMemory(p.getId(), new AgentMemory());
			this.memories.getMemory(p.getId()).setHomeZone(origin);
		}
		return personCnt;
	}
	
	private void createDemandForPerson(PersonImpl person, String day) {
		PlanImpl plan = new PlanImpl();
		ActivityFacility home = this.homeLocations.get(person.getId());
		
		ActivityImpl act = new ActivityImpl("home", home.getCoord());
		act.setFacilityId(home.getId());
		
		double endTime = Math.max(0.0, this.random.nextGaussian() * 2.0 * 3600.0 + 7.0 * 3600.0);
		act.setEndTime(endTime);
		plan.addActivity(act);
		plan.addLeg(new LegImpl("car"));
		
		endTime = this.addWorkingAct(plan, person, day, endTime);
		endTime = this.addOtherActs(plan, person, day, endTime);
		
		this.finishPlan(plan, person);		
		this.memories.getMemory(person.getId()).addPlan(plan, day);
	}
	
	private double addOtherActs(PlanImpl plan, PersonImpl person, String day, double endTime) {
		DecisionModel decisionModel = this.decisionModels.getDecisionModelForAgent(person.getId());
		boolean checkShopping = false;
		boolean checkLeisure = false;
		
		if (this.random.nextBoolean()) {
			checkShopping = true;
		}
		if (this.random.nextBoolean()) {
			checkLeisure = true;
		}
				
		if (decisionModel.doesAct("shop", day) && checkShopping) {
			ActivityFacility facility = this.memories.getMemory(person.getId()).getHomeZone().getRandomLocationInZone(random);
			ActivityImpl act = new ActivityImpl("shop", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 1.5 * 3600.0 + 1.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
			plan.addLeg(new LegImpl("car"));
		}
		// check leisure
		if (decisionModel.doesAct("leisure", day) && checkLeisure) {
			TreeMap<Id, ActivityFacility> facilitiesLeisure = this.scenario.getActivityFacilities().getFacilitiesForActivityType("leisure");
			ActivityFacility facility = (ActivityFacility) facilitiesLeisure.values().toArray()[random.nextInt(facilitiesLeisure.size())];
			ActivityImpl act = new ActivityImpl("leisure", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 2.0 * 3600.0 + 3.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
			plan.addLeg(new LegImpl("car"));
		}
		return endTime;
	}
	
	private double addWorkingAct(PlanImpl plan, PersonImpl person, String day, double endTime) {
		DecisionModel decisionModel = this.decisionModels.getDecisionModelForAgent(person.getId());
		
		if (decisionModel.doesAct("work", day)) {
			ActivityFacility facility = this.workLocations.get(person.getId());
			ActivityImpl workAct = new ActivityImpl("work", facility.getCoord());
			endTime += Math.max(0.5 * 3600.0, this.random.nextGaussian() * 1.5 * 3600.0 + 8.0 * 3600.0);
			workAct.setEndTime(endTime);
			workAct.setFacilityId(facility.getId());
			plan.addActivity(workAct);
			plan.addLeg(new LegImpl("car"));
		}
		return endTime;
	}
	
	private void finishPlan(PlanImpl plan, PersonImpl person) {
		ActivityFacility home = this.homeLocations.get(person.getId());
		ActivityImpl homeAct = new ActivityImpl("home", home.getCoord());
		homeAct.setFacilityId(home.getId());
		plan.addActivity(homeAct);
	}
							
	public void write(String path) {
		log.info("Writing population ...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(path + "/plans.xml");
	}

	public Zone getTollZone() {
		return tollZone;
	}

	public void setTollZone(Zone tollZone) {
		this.tollZone = tollZone;
	}
}
