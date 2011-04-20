/* *********************************************************************** *
 * project: org.matsim.CreateNewZHScenario
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

package herbie.creation;

import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.population.filters.PersonIntersectAreaFilter;
import org.matsim.core.population.ActivityImpl;

import utils.BuildTrees;

public class CreateNewZHScenario {
	private final static Logger log = Logger.getLogger(CreateNewZHScenario.class);
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String outputFolder;
	private String networkfilePath;
	private String facilitiesfilePath;
	private String plansV2filePath;
	
	// cross-border
	private String crossBorderPlansFilePath;
	
	//freight zh
	private String freightPlansFilePath;
	
	private double sampleRatePercent = 100.0;
	
	// ====================================================================================
	public static void main(final String[] args) {
		if (args.length != 1) {
			log.error("Please specify a config file!");
			return;
		}
		log.info("Creation started ...");
		CreateNewZHScenario creator = new CreateNewZHScenario();
		creator.run(args[0]);
		log.info("Creation finished -----------------------------------------");
	}
	
	// ====================================================================================
	private void run(String configFile) {
		this.init(configFile);
		this.addSpecialPlans2Population(this.crossBorderPlansFilePath, "cross-border");
		this.addSpecialPlans2Population(this.freightPlansFilePath, "freight");
		this.write();
	}
	
	// ====================================================================================
	// read in network, facilities and plans into scenario
	private void init(String configFile) {
		log.info("Initializing ...");
		this.readConfig(configFile);
		
		log.info("Reading network, facilities and plans ...");
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansV2filePath);
	}
	
	private void readConfig(String configFile) {
		log.info("Reading the config ...");
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(configFile);   	
		
		this.outputFolder = config.findParam("demandcreation", "outputFolder");
		
		// old scenario parts -----
		this.networkfilePath = config.findParam("demandcreation", "networkFile");
		this.facilitiesfilePath = config.findParam("demandcreation", "facilitiesFile");
		this.plansV2filePath = config.findParam("demandcreation", "plansV2File");
		
		// new demand -------------
		this.crossBorderPlansFilePath = config.findParam("demandcreation", "crossBorderPlansFile");
		this.freightPlansFilePath = config.findParam("demandcreation", "freightPlansFile");
		
		this.sampleRatePercent = Double.parseDouble(config.findParam("demandcreation", "samplingRatePercent"));
    }
	
	// ====================================================================================
	// read cross-border plans and add them to the scenario
	// the cross border facilities are already integrated in the facilities
	private void addSpecialPlans2Population(String plansFilePath, String type) {
		ScenarioImpl sTmp = (ScenarioImpl) ScenarioUtils.createScenario(
				ConfigUtils.createConfig());
		
		new MatsimNetworkReader(sTmp).readFile(networkfilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(sTmp);
		populationReader.readFile(plansFilePath);
		
		this.map2Network((PopulationImpl) sTmp.getPopulation());
		
		if (type.equals("cross-border")) {
			log.info("Adding cross-border traffic ...");
			this.mapActivities2Facilities((PopulationImpl) sTmp.getPopulation());
			List<Id> persons2remove = this.dilutedZH(sTmp.getPopulation());
			for (Id personId : persons2remove) {
				sTmp.getPopulation().getPersons().remove(personId);
			}
		}
		
		// facilities are already mapped
		if (type.equals("freight")) {
			log.info("Adding freight traffic ...");
			List<Id> persons2remove = this.dilutedZH(sTmp.getPopulation());
			for (Id personId : persons2remove) {
				sTmp.getPopulation().getPersons().remove(personId);
			}
		}
		this.samplePlans(sTmp.getPopulation(), this.sampleRatePercent);
		log.info("Remaining " + type + " agents" + sTmp.getPopulation().getPersons().size());
		
		for (Person p : sTmp.getPopulation().getPersons().values()){
			this.scenario.getPopulation().addPerson(p);
		}
		log.info("Number of agents in new scenario: " + this.scenario.getPopulation().getPersons().size());
	} 
	
	//TODO: Is this really necessary?
	// What happens if we have small differences in facilities->links mapping during simulation?
	private void mapActivities2Facilities(PopulationImpl population) {
		log.info("Mapping facilities ...");
		TreeMap<String, QuadTree<ActivityFacility>> trees = new TreeMap<String, QuadTree<ActivityFacility>>();
		
		for (Person p : population.getPersons().values()){
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						ActivityImpl act = (ActivityImpl)pe;
						
						BuildTrees util = new BuildTrees();
						if (trees.get(act.getType()) == null) trees.put(act.getType(), util.createActivitiesTree(act.getType(), this.scenario));
						QuadTree<ActivityFacility> facQuadTree = trees.get(act.getType());
						
						// get closest facility.
						ActivityFacility facility = facQuadTree.get(act.getCoord().getX(), act.getCoord().getY());
						act.setFacilityId(facility.getId());
					}
				}
			}
		}
	}	
	// mapping the activities to this.network
	// the normal plans v2 are already mapped
	private void map2Network(PopulationImpl population) {	
		XY2Links mapper = new XY2Links(this.scenario.getNetwork());
		for (Person p : population.getPersons().values()){
			mapper.run(p);
		}
	}
	
	// at the moment only necessary for cross-border and freight traffic -> stratified sampling
	private void samplePlans(Population population, double percent) {
		log.info("Sampling plans ...");
		int newPopulationSize = (int)(population.getPersons().size() * percent);
		
		while (population.getPersons().size() > newPopulationSize) {
			Random random = new Random();
			int index = random.nextInt(population.getPersons().size());
			population.getPersons().remove(new IdImpl(index));
		}
	}
	
	// at the moment only necessary for cross-border traffic
	private List<Id> dilutedZH(Population population) {
		log.info("Cutting scenario ...");
		double aoiRadius = 30000.0;
		final CoordImpl aoiCenter = new CoordImpl(683518.0,246836.0);
		
		List<Id> persons2remove = new Vector<Id>();
		
		PersonIntersectAreaFilter filter = new PersonIntersectAreaFilter(null, null, this.scenario.getNetwork());
		filter.setAlternativeAOI(aoiCenter, aoiRadius);
		
		for (Person person : population.getPersons().values()) {
			if (!filter.judge(person)) {
				persons2remove.add(person.getId());
			}
		}
		return persons2remove;
	}
		
	private void write() {
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.outputFolder + "plans.xml.gz");
	}
}
