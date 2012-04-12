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

package playground.anhorni.surprice.preprocess.rwscenario;

import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.anhorni.surprice.Surprice;
import playground.balmermi.census2000.modules.PersonSetSecLoc;

public class Merger {	
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());	
	private ConvertThurgau2Plans thurgauConverter = new ConvertThurgau2Plans();
	private TreeMap<Id, PersonHomeWork> personHWFacilities = new TreeMap<Id, PersonHomeWork>();
	private Random random = new Random(102830259L);
	private TreeMap<Id, PersonWeeks> personWeeksMZ = new TreeMap<Id, PersonWeeks>();
		
	private final static Logger log = Logger.getLogger(ConvertThurgau2Plans.class);
		
	public static void main(final String[] args) {		
		if (args.length != 1) {
			log.error("Provide correct number of arguments ...");
			System.exit(-1);
		}		
		Merger creator = new Merger();
		creator.run(args[0]);
	}
	
	public void run(String configFile) {
		Config config = ConfigUtils.loadConfig(configFile);	
		
		// handle MZ ...........................................................
		this.readMZ(config.getModule("plans").getValue("inputPlansFile"), 
				config.getModule("network").getValue("inputNetworkFile"), 
				config.getModule("facilities").getValue("inputFacilitiesFile"));
		
		this.storeHomeAndWork();
		
		// handle Thurgau ......................................................
		thurgauConverter.run(
				config.findParam(Surprice.SURPRICE_PREPROCESS, "infileF2"),
				config.findParam(Surprice.SURPRICE_PREPROCESS, "infileF3"),
				config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
		
		// merge ................................................................
		this.merge();
		
		this.writeWeek(config.findParam(Surprice.SURPRICE_PREPROCESS, "outPath"));
	}
	
	private void writeWeek(String outPath) {
		for (int dow = 0; dow < 7; dow++) {
			int counter = 0;
			int nextMsg = 1;
			for (Person person : this.scenario.getPopulation().getPersons().values()) {			
				counter++;
				if (counter % nextMsg == 0) {
					nextMsg *= 2;
					log.info(" person # " + counter);
				}
				person.getPlans().clear();
				Plan plan = this.personWeeksMZ.get(person.getId()).getDay(dow, 0);
				person.addPlan(plan);
				((PersonImpl)person).setSelectedPlan(plan);				
			}
			log.info("Writing population with plans ...");
			new PopulationWriter(this.scenario.getPopulation(), scenario.getNetwork()).write(outPath + "/" + dow + "plans.xml.gz");
		}	
	}
		
	private void readMZ(final String plansFilePath, final String networkFilePath, final String facilitiesFilePath) {
		new MatsimNetworkReader(scenario).readFile(networkFilePath);		
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}
	
	private void storeHomeAndWork() {
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {			
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			PlanImpl plan = (PlanImpl)p.getSelectedPlan();
			Id homeFacilityId = plan.getFirstActivity().getFacilityId();
			Id workFacilityId = this.getWorkFacilityId(plan);			
			this.personHWFacilities.put(p.getId(), new PersonHomeWork(p, homeFacilityId, workFacilityId));
		}
	}
	
	private Id getWorkFacilityId(PlanImpl plan) {
		Id workId = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe;				
				if (act.getType().startsWith("w")) {
					workId = act.getFacilityId();
				}
			}
		}
		return workId;
	}
	
	private void merge() {
		// prepare data structures for probabilistic draw of chains
		// pweights + workers and non-workers
		TreeMap<Double, PersonWeeks> workersNormalized = new TreeMap<Double, PersonWeeks>(java.util.Collections.reverseOrder());
		TreeMap<Double, PersonWeeks> nonworkersNormalized = new TreeMap<Double, PersonWeeks>(java.util.Collections.reverseOrder());		
		this.prepareForDrawing(workersNormalized, nonworkersNormalized);
						
		// assign chain (acts + times) ----------------
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {			
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}			
			PersonWeeks personWeeksThurgau;
			if (this.personHWFacilities.get(p.getId()).getWorkFaciliyId() != null) {
				personWeeksThurgau = this.chooseWeek(workersNormalized);
			}
			else {
				personWeeksThurgau = this.chooseWeek(nonworkersNormalized);
			}
			PersonImpl person = (PersonImpl)p;			
			this.createPlansForPerson(person, personWeeksThurgau);
		}				
	}
	
	private void createPlansForPerson(PersonImpl person, PersonWeeks personWeeksThurgau) {
		// only one week to begin with
		int week = 0;
		for (int dow = 0; dow < 7; dow++) {
			person.getPlans().clear();
			Plan plan = personWeeksThurgau.getDay(dow, week);
			PersonImpl thurgauPerson = (PersonImpl)personWeeksThurgau.getPerson();
			thurgauPerson.addPlan(plan);
			thurgauPerson.setSelectedPlan(plan);
			Plan planNew = thurgauPerson.copySelectedPlan();
			
			person.addPlan(planNew);
			person.setSelectedPlan(planNew);

			// TODO: smear times
			
			// assign home and work locations
			PersonHomeWork phw = this.personHWFacilities.get(person.getId());			
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;				
					if (act.getType().startsWith("w")) {
						act.setFacilityId(phw.getWorkFaciliyId());
						act.setCoord(this.scenario.getActivityFacilities().getFacilities().get(phw.getWorkFaciliyId()).getCoord());
						act.setLinkId(this.scenario.getActivityFacilities().getFacilities().get(phw.getWorkFaciliyId()).getLinkId());
					}
					else if (act.getType().startsWith("h")) {
						act.setFacilityId(phw.getHomeFacilityId());
						act.setCoord(this.scenario.getActivityFacilities().getFacilities().get(phw.getHomeFacilityId()).getCoord());
						act.setLinkId(this.scenario.getActivityFacilities().getFacilities().get(phw.getHomeFacilityId()).getLinkId());
					}					
				}
			}
			// assign shop, leisure and education locations according to Balmers neighborhood search
			PersonSetSecLoc secLocationAssigner = new PersonSetSecLoc(this.scenario.getActivityFacilities(), null);
			secLocationAssigner.run(person);
			
			if (this.personWeeksMZ.get(person.getId()) == null) {
				this.personWeeksMZ.put(person.getId(), new PersonWeeks());
			}
			this.personWeeksMZ.get(person.getId()).addDay(dow, person.getSelectedPlan());
		}
	}
	
	private PersonWeeks chooseWeek(TreeMap<Double, PersonWeeks> map) {				
		// score 0 is included as random range = 0.0d (inclusive) to 1.0d (exclusive)		
		for (int i = 0; i < 10; i++) {
			random.nextDouble();
		}
		double randomScore = random.nextDouble();
				
		PersonWeeks personWeeks = map.get(map.firstKey());
		for (Entry<Double, PersonWeeks> entry : map.entrySet()) {
	        if (entry.getKey() > randomScore + 0.000000000000000001) {
	        	personWeeks = entry.getValue();
	        }
	    }		
		return personWeeks;
	}
	
	private void prepareForDrawing(TreeMap<Double, PersonWeeks> workersNormalized, TreeMap<Double, PersonWeeks> nonworkersNormalized) {
		TreeMap<Id, PersonWeeks> workers = new TreeMap<Id, PersonWeeks>();
		TreeMap<Id, PersonWeeks> nonworkers = new TreeMap<Id, PersonWeeks>();		
		for (Id pid : thurgauConverter.getPersonWeeks().keySet()) {			
			if (thurgauConverter.getPersonWeeks().get(pid).isWorker()) {
				workers.put(pid, thurgauConverter.getPersonWeeks().get(pid));
			}
			else {
				nonworkers.put(pid, thurgauConverter.getPersonWeeks().get(pid));
			}
		}
		// normalize maps:
		this.normalizeMap(workersNormalized, workers);
		this.normalizeMap(nonworkersNormalized, nonworkers);
	}
	
	private double getTotalScore(TreeMap<Id, PersonWeeks> map) {
		double totalScore = 0.0;
		for (PersonWeeks personWeeks : map.values()) {				
			double score = personWeeks.getPweight();
			totalScore += score;
		}
		return totalScore;
	}
	
	private void normalizeMap(TreeMap<Double, PersonWeeks> mapNormalized, TreeMap<Id, PersonWeeks> map) {
		double sumScore = 0.0;
		for (PersonWeeks personWeeks : map.values()) {
			double score = personWeeks.getPweight();
			sumScore += (score / this.getTotalScore(map));				
			mapNormalized.put(sumScore , personWeeks);	
		}
	}	
}
