/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.surprice;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.utils.objectattributes.ObjectAttributes;
//import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
//import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;


public class WeekScenario {	
	private String [] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
	private TreeMap<Id, AgentMemory> memories = new TreeMap<Id, AgentMemory>();
	private TreeMap<Id, DecisionModel> decisionModels = new TreeMap<Id, DecisionModel>();
	
	private Config config;
	
//	private ObjectAttributes agentsMemoriesOA = new ObjectAttributes();
	private ScenarioImpl baseScenario;
	private ScenarioImpl saturdayScenario;
	private ScenarioImpl sundayScenario;
	
	private String baseOutDir;
	
	private final static Logger log = Logger.getLogger(WeekScenario.class);
		
	public static void main (final String[] args) {
		if (args.length != 1) {
			log.error("Please specify path to config file");
			System.exit(-1);
		}
		WeekScenario scenario = new WeekScenario();
		scenario.run(args[0]);
    }

	public void run(String path) {
		this.init(path);
		
		for ( int i = 0; i < days.length; i++) {
			if (days[i].equals("Tue")) {
				this.initAfterMonday();
			}
			this.runDay(days[i]);
		}
		log.info("######################### Week simulated ################################");
	}
	
	public void init(String path) {		
		this.config = ConfigUtils.loadConfig(path);		
		this.baseOutDir = this.config.getParam("controler", "outputDirectory");		
		this.baseScenario = (ScenarioImpl) ScenarioUtils.loadScenario(this.config);
		
		// TODO: replace by sat and sun configs
		this.saturdayScenario = (ScenarioImpl) ScenarioUtils.loadScenario(this.config);
		this.sundayScenario = (ScenarioImpl) ScenarioUtils.loadScenario(this.config);
	}
	
	public void initAfterMonday() {
		this.createPlansPool();
		this.initDecisionModels();
	}
	
	public void runDay(String day) {		
		if (!(day.equals("Mon") || day.equals("Sat") || day.equals("Sun"))) {
			this.assignPlans2Agents(day);
		}
		// set path to next day
		String outdir = this.baseOutDir + "/" + day;
		this.config.setParam("controler", "outputDirectory", outdir);
				
		DayControler controler = null;
		
		if (day.equals("Sat")) {
			controler = new DayControler(this.saturdayScenario);
		}
		else if (day.equals("Sun")) {
			controler = new DayControler(this.sundayScenario);
		}
		else {
			controler = new DayControler(this.baseScenario); 
		}
		controler.run();		
		
		this.addPlans2Memory();
		this.clearPlans();
	}
	
	private void createPlansPool() {
//    	// create week pool
//    	this.weekPool = new PlanPool();
//    	this.weekPool.create(this.baseScenario.getPopulation());
//    	
//    	// create weekend pools
//    	this.saturdayPool = new PlanPool();  
//    	this.saturdayPool.create(this.saturdayScenario.getPopulation());
//    	
//    	this.sundayPool = new PlanPool();  
//    	this.sundayPool.create(this.sundayScenario.getPopulation());    	
    }
	
	private void addPlans2Memory() {
		for (Person p : this.baseScenario.getPopulation().getPersons().values()) {
			if (this.memories.get(p.getId()) == null) {
				this.memories.put(p.getId(), new AgentMemory());
			}
			//this.memories.get(p.getId()).addPlan(p.getSelectedPlan());
		}
	}
	
	private void clearPlans() {
		for (Person p : this.baseScenario.getPopulation().getPersons().values()) {
			p.getPlans().clear();
		}
	}
	
	private void assignPlans2Agents(String day) {
		for (Person p : this.baseScenario.getPopulation().getPersons().values()) {
			Plan plan = this.drawPlanFromPool(day, p.getId(), this.decisionModels.get(p.getId()));
			p.addPlan(plan);
			((PersonImpl)p).setSelectedPlan(plan);
		}
	}
	
	private DecisionModel initDecisionModels() {
		return new DecisionModel();
	}
	
    private Plan drawPlanFromPool(String day, Id personId, DecisionModel decisionModel) {    	
//    	if (day.equals("Sat")) {
//    		return this.saturdayPool.getPlan(this.memories.get(personId), decisionModel);	
//	    }
//    	else if (day.equals("Sun")) {
//    		return this.sundayPool.getPlan(this.memories.get(personId), decisionModel);
//    	}
//	    else {
//	    	return this.weekPool.getPlan(this.memories.get(personId), decisionModel);	
//	    }   
    	return null;
    }
    
//    private void writeAgentsMemories() {
//    	ObjectAttributesXmlWriter attributesWriter = new ObjectAttributesXmlWriter(this.agentsMemoriesOA);
//		attributesWriter.writeFile("path to memories");    	
//    }
//        
//    private void readAgentsMemories() {
//    	ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.agentsMemoriesOA);
//		attributesReader.parse("path to memories");
//    }
        
}
