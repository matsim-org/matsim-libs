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

package playground.anhorni.LEGO.miniscenario.create;

import java.util.Vector;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;

import playground.anhorni.LEGO.miniscenario.ConfigReader;
import playground.anhorni.random.RandomFromVarDistr;

public class CreatePopulation {
	private ScenarioImpl scenario = null;	
	private ConfigReader configReader = null;
	private Config config;
	private final static Logger log = Logger.getLogger(CreatePopulation.class);
	private RandomFromVarDistr rnd;
			
	public void createPopulation(ScenarioImpl scenario, ConfigReader configReader, RandomFromVarDistr rnd, Config config) {		
		this.scenario = scenario;
		this.configReader = configReader;
		this.config = config;
		this.rnd = rnd;
		
		this.addPersons();
		this.assignTasteValues();
		
		log.info("Finishing plans ...");
		this.finishPlans();
		this.removeNonAnalysisPersons();
			
		ComputeMaxEpsilons maxEpsilonComputer = new ComputeMaxEpsilons(10, scenario, "shop", configReader, config);
		maxEpsilonComputer.prepareReplanning();
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			maxEpsilonComputer.handlePlan(p.getSelectedPlan());
		}
		maxEpsilonComputer.finishReplanning();
		
		maxEpsilonComputer = new ComputeMaxEpsilons(10, scenario, "leisure", configReader, config);
		maxEpsilonComputer.prepareReplanning();
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			maxEpsilonComputer.handlePlan(p.getSelectedPlan());
		}
		maxEpsilonComputer.finishReplanning();
	}
						
	private void addPersons() {	
		int personCnt = 0;
//		int boundaryFacilitiesCnt = 0;	
		int analysisPopulationCnt = 0;
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilitiesForActivityType("home").values()) {
			
			// check if home facility in free boundary zone
//			if ((facility.getCoord().getX() < 499.0 || facility.getCoord().getX() > configReader.getSideLengt() - 1.0 || 
//					facility.getCoord().getY() < 499.0 || facility.getCoord().getY() > configReader.getSideLengt() - 1.0)) {
//				boundaryFacilitiesCnt++;
//				continue;
//			}
			
			// check if person is in the analysis population
			int offset = configReader.getAnalysisPopulationOffset();
			if ((facility.getCoord().getX() >= (configReader.getSideLengt() / 2.0) - 500.0
					&& facility.getCoord().getX() <= (configReader.getSideLengt() / 2.0) + 500.0) && 
					(facility.getCoord().getY() >= (configReader.getSideLengt() / 2.0) - 500.0
					&& facility.getCoord().getY() <= (configReader.getSideLengt() / 2.0) + 500.0)) {
				offset = 0;
				analysisPopulationCnt++;
			}
						
			for (int j = 0; j < this.configReader.getPersonsPerLocation(); j++) {
				PersonImpl p = new PersonImpl(new IdImpl(personCnt + offset));
				personCnt++;
				p.createAndAddPlan(true);
				ActivityImpl act = new ActivityImpl("home", facility.getCoord());
				act.setFacilityId(facility.getId());
				act.setEndTime(11.0 * 3600.0);
				p.getSelectedPlan().addActivity(act);
				p.getSelectedPlan().addLeg(new LegImpl("car"));
				
				p.createDesires("");				
				this.scenario.getPopulation().addPerson(p);
			}
		}
		log.info("Created " + personCnt + " persons including " + analysisPopulationCnt * this.configReader.getPersonsPerLocation() + " analysis persons");
	}
		
	private void finishPlans() {
		//RandomFromVarDistr rnd = new RandomFromVarDistr();
		//double maxWorkDistance = 500.0;
		//double maxShopDistance = 2500.0;
		//Bins workBins = new Bins(configReader.getSpacing() * 2, maxWorkDistance, "work distance");
		//Bins shopBins = new Bins(configReader.getSpacing() * 2, maxShopDistance, "shop distance");
		
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			
			// show counter
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			PlanImpl plan = (PlanImpl)p.getSelectedPlan(); 
			
			//double workDistance = 0.0; //rnd.getNegLinear(maxWorkDistance);
			//ActivityFacility workFacility = this.assignLocation("work", workDistance, plan.getFirstActivity().getCoord());
			// if person is not in the analysis population
			//if (Integer.parseInt(p.getId().toString()) > configReader.getAnalysisPopulationOffset()) {
			//	workBins.addVal(((CoordImpl)workFacility.getCoord()).calcDistance(plan.getFirstActivity().getCoord()), 1.0);
			//}
			
			//ActivityImpl workAct = new ActivityImpl("work", workFacility.getCoord());
			//workAct.setEndTime(17.0 * 3600);
			//workAct.setFacilityId(workFacility.getId());
			//p.getSelectedPlan().addActivity(workAct);
			//p.getSelectedPlan().addLeg(new LegImpl("car"));
			
			//double shopDistance = rnd.getUniform(maxShopDistance);
			//ActivityFacility shopFacility = this.assignLocation("shop", shopDistance, workFacility.getCoord());
			
			// if person is not in the analysis population
			//if (Integer.parseInt(p.getId().toString()) > configReader.getAnalysisPopulationOffset()) {
			//	shopBins.addVal(((CoordImpl)shopFacility.getCoord()).calcDistance(workFacility.getCoord()), 1.0);
			//}
			ActivityImpl shopAct = new ActivityImpl("shop", plan.getFirstActivity().getCoord());
			shopAct.setFacilityId(plan.getFirstActivity().getFacilityId());
			shopAct.setEndTime(12.0 * 3600.0);
			p.getSelectedPlan().addActivity(shopAct);
			p.getSelectedPlan().addLeg(new LegImpl("car"));
			
			ActivityImpl homeAct = new ActivityImpl("home", plan.getFirstActivity().getCoord());
			homeAct.setFacilityId(plan.getFirstActivity().getFacilityId());
			p.getSelectedPlan().addActivity(homeAct);		
		}
		//workBins.plotBinnedDistribution(configReader.getPath() + "input/workDistances", "#", "m");
		//shopBins.plotBinnedDistribution(configReader.getPath() + "input/shopDistances", "#", "m");
	}
	
//	private ActivityFacility assignLocation(String type, double distance, Coord startCoordinates) {
//		int distanceIndex = (int)(distance /configReader.getSpacing());
//		Vector<ActivityFacility> facilities = new Vector<ActivityFacility>();
//		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilitiesForActivityType(type).values()) {
//			double distanceTmp = ((CoordImpl)startCoordinates).calcDistance(facility.getCoord());
//			if (distanceTmp <= distance && distanceTmp >= distanceIndex * configReader.getSpacing()) {
//				facilities.add(facility);
//			}
//		}
//		Collections.shuffle(facilities);
//		return facilities.get(0);
//	}
	
	private void assignTasteValues() {
		HandleUnobservedHeterogeneity handler = new HandleUnobservedHeterogeneity(scenario, configReader, rnd, config);
		handler.assign();
	}
				
	public void write(String path) {
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(path + "plans.xml");
	}
	
	// better quality of assignment of tastes!
	private void removeNonAnalysisPersons() {
		Vector<Id> ids2remove = new Vector<Id>();
		for (Person p : this.scenario.getPopulation().getPersons().values()) {
			// if person is not in the analysis population
			if (Integer.parseInt(p.getId().toString()) >= configReader.getAnalysisPopulationOffset()) {
				ids2remove.add(p.getId()); 
			}
		}
		for (Id id : ids2remove) {
			this.scenario.getPopulation().getPersons().remove(id);
		}
		log.info("Removed " + ids2remove.size() + " non-analysis persons ...");
	}
}
