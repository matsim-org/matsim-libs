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

package playground.anhorni.surprice.preprocess.miniscenario;

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
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.anhorni.surprice.AgentMemories;
import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.DecisionModel;
import playground.anhorni.surprice.DecisionModels;
import playground.anhorni.surprice.Surprice;
import playground.anhorni.surprice.preprocess.Zone;

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
	private ObjectAttributes incomes = new ObjectAttributes();
	
	private ObjectAttributes preferences = new ObjectAttributes();
				
	public void createPopulation(ScenarioImpl scenario, Config config) {		
		this.scenario = scenario;
		this.config = config;		
		this.createPersons();
						
		for (String day : Surprice.days) {
			for (Person p : this.scenario.getPopulation().getPersons().values()) {	
				DecisionModel decisionModel = this.decisionModelCreator.createDecisionModelForAgent((PersonImpl)p, this.memories.getMemory(p.getId()));
				this.decisionModels.addDecsionModelForAgent(decisionModel, p.getId());
				this.createDemandForPerson((PersonImpl)p, day);	
			}
		}
		for (String day : Surprice.days) {
			// choose random plan for day
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				// set plan and remove it from memory
				Plan plan = this.memories.getMemory(person.getId()).getRandomPlanAndRemove(day, this.random);
				((PersonImpl)person).addPlan(plan);
				((PersonImpl)person).setSelectedPlan(plan);
				
				this.preferences.putAttribute(person.getId().toString(), day, this.random.nextDouble());
			}
			String outPath = config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath") + day;
			new File(outPath).mkdirs();
			this.write(outPath);
			
			// clear plan for day from persons
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				((PersonImpl)person).getPlans().clear();
			}
		}
		this.writeIncomes(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
		this.writePreferences(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
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
		double sideLength = Double.parseDouble(config.findParam(Surprice.SURPRICE_PREPROCESS, "sideLength"));
		Zone centerZone = new Zone("centerZone", (Coord) new CoordImpl(sideLength / 2.0 - 500.0, sideLength / 2.0 + 500.0), 1000.0, 1000.0);	
		this.initZone(centerZone);
		
		this.tollZone = centerZone;
		
		Zone topLeftZone =  new Zone("topLeftZone", (Coord) new CoordImpl(0.0, sideLength), 1000.0, 1000.0); 
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
		int personsPerLocation = Integer.parseInt(config.findParam(Surprice.SURPRICE_PREPROCESS, "personsPerZone"));
		int personCnt = 0;
		for (int j = 0; j < personsPerLocation; j++) {
			PersonImpl p = new PersonImpl(new IdImpl(personCnt + offset));			
			personCnt++;
			this.scenario.getPopulation().addPerson(p);	
			ActivityFacility home = origin.getRandomLocationInZone(this.random);
			this.homeLocations.put(p.getId(), home);
						
			ActivityFacility work = destination.getRandomLocationInZone(this.random);
			this.workLocations.put(p.getId(), work);
			
			// assign daily income according to one-sided Gauss distribution. 
			// TODO: check Gini coefficient, Lorenz distribution
			double r = Math.abs(random.nextGaussian() * Surprice.stdDev + Surprice.mean);						
			
			if (Boolean.parseBoolean(config.findParam(Surprice.SURPRICE_PREPROCESS, "richpoor"))) {
				if (origin.getName().equals("centerZone")) { // poor area
					r *= 0.8;
				} 
				else if (origin.getName().equals("bottomLeftZone")) { // rich area
					r *= 1.0 / 0.8;
				}
			}
			
			String income = Double.toString(r);
			this.incomes.putAttribute(p.getId().toString(), "income", income);
			
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
				
		endTime = this.addWorkingAct(plan, person, day, endTime);
		endTime = this.addOtherActs(plan, person, day, endTime);
		
		this.finishPlan(plan, person);		
		this.memories.getMemory(person.getId()).addPlan(plan, day);
	}
	
	private double addOtherActs(PlanImpl plan, PersonImpl person, String day, double endTime) {
		//DecisionModel decisionModel = this.decisionModels.getDecisionModelForAgent(person.getId());
		boolean checkShopping = false;
		boolean checkLeisure = false;
		
		if (this.random.nextBoolean()) {
			checkShopping = true;
		}
		else {
			checkLeisure = true;
		}
		if (this.random.nextBoolean()) {
			checkLeisure = true;
		}
		else {
			checkShopping = true;
		}
				
		//if (decisionModel.doesAct("shop", day) && checkShopping) {
		if (checkShopping) {
			plan.addLeg(new LegImpl("car"));
			ActivityFacility facility = this.memories.getMemory(person.getId()).getHomeZone().getRandomLocationInZone(random);
			ActivityImpl act = new ActivityImpl("shop", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 1.5 * 3600.0 + 1.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
		}
		// check leisure
		//if (decisionModel.doesAct("leisure", day) && checkLeisure) {
		if (checkLeisure) {
			plan.addLeg(new LegImpl("car"));
			TreeMap<Id, ActivityFacility> facilitiesLeisure = this.scenario.getActivityFacilities().getFacilitiesForActivityType("leisure");
			ActivityFacility facility = (ActivityFacility) facilitiesLeisure.values().toArray()[random.nextInt(facilitiesLeisure.size())];
			ActivityImpl act = new ActivityImpl("leisure", facility.getCoord());
			endTime += Math.max(0.1 * 3600.0, this.random.nextGaussian() * 2.0 * 3600.0 + 3.0 * 3600.0);
			act.setEndTime(endTime);
			act.setFacilityId(facility.getId());
			plan.addActivity(act);
		}
		return endTime;
	}
	
	private double addWorkingAct(PlanImpl plan, PersonImpl person, String day, double endTime) {
		DecisionModel decisionModel = this.decisionModels.getDecisionModelForAgent(person.getId());
		
		if (decisionModel.doesAct("work", day)) {
			plan.addLeg(new LegImpl("car"));
			ActivityFacility facility = this.workLocations.get(person.getId());
			ActivityImpl workAct = new ActivityImpl("work", facility.getCoord());
			endTime += Math.max(0.5 * 3600.0, this.random.nextGaussian() * 1.5 * 3600.0 + 8.0 * 3600.0);
			workAct.setEndTime(endTime);
			workAct.setFacilityId(facility.getId());
			plan.addActivity(workAct);
		}
		return endTime;
	}
	
	private void finishPlan(PlanImpl plan, PersonImpl person) {			
		plan.addLeg(new LegImpl("car"));
		ActivityFacility home = this.homeLocations.get(person.getId());
		ActivityImpl homeAct = new ActivityImpl("home", home.getCoord());
		homeAct.setFacilityId(home.getId());
		plan.addActivity(homeAct);
	}
							
	public void write(String path) {
		log.info("Writing plans ...");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(path + "/plans.xml");
	}
	
	private void writeIncomes(String path) {
		log.info("Writing incomes to " + path + "/incomes.xml");
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.incomes);
		attributesWriter.writeFile(path + "/incomes.xml"); 
	}
	
	private void writePreferences(String path) {
		log.info("Writing preferences to " + path + "/preferences.xml");
		ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.preferences);
		attributesWriter.writeFile(path + "/preferences.xml"); 
	}

	public Zone getTollZone() {
		return tollZone;
	}

	public void setTollZone(Zone tollZone) {
		this.tollZone = tollZone;
	}
}
