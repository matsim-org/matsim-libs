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

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.locationchoice.utils.ActTypeConverter;
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
		log.info("\tCreation started ............................................");
		CreateNewZHScenario creator = new CreateNewZHScenario();
		creator.run(args[0]);
		log.info("\tCreation finished -----------------------------------------");
	}
	
	// ====================================================================================
	private void run(String configFile) {
		this.init(configFile);
		log.info("\tAdding cross-border traffic ............................");
		this.addSpecialPlans2Population(this.crossBorderPlansFilePath, "cross-border");
		log.info("\tAdding freight traffic .................................");
		this.addSpecialPlans2Population(this.freightPlansFilePath, "freight");
		this.write();
	}
	
	// ====================================================================================
	// read in network, facilities and plans into scenario
	private void init(String configFile) {
		log.info("\tInitializing ................................................");
		this.readConfig(configFile);
		
		log.info("\tReading network, facilities and plans .............................");
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansV2filePath);
	}
	
	private void readConfig(String configFile) {
		log.info("\tReading the config .........................");
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
			log.info(sTmp.getPopulation().getPersons().size() + " cross-border agents ##########################################");
			this.convertFromV1toV2(sTmp);
			this.mapActivities2Facilities((PopulationImpl) sTmp.getPopulation());
			List<Id> persons2remove = this.dilutedZH(sTmp.getPopulation());
			for (Id personId : persons2remove) {
				sTmp.getPopulation().getPersons().remove(personId);
			}
			log.info("\tRemaining " + type + " agents " + sTmp.getPopulation().getPersons().size() + " ###############################");
		}
		
		// facilities are already mapped
		if (type.equals("freight")) {
			log.info(sTmp.getPopulation().getPersons().size() + " freight agents #######################################");
			List<Id> persons2remove = this.dilutedZH(sTmp.getPopulation());
			for (Id personId : persons2remove) {
				sTmp.getPopulation().getPersons().remove(personId);
			}
			log.info("\tRemaining " + type + " agents " + sTmp.getPopulation().getPersons().size());
		}
		this.samplePlans(sTmp.getPopulation(), this.sampleRatePercent);
		log.info("\tRemaining " + type + " agents " + sTmp.getPopulation().getPersons().size() + " ####################################");
		
		for (Person p : sTmp.getPopulation().getPersons().values()){
			this.scenario.getPopulation().addPerson(p);
		}
		log.info("\tNumber of agents in new scenario: " + this.scenario.getPopulation().getPersons().size() + " #################################");
	} 
	
	//TODO: Is this really necessary? Or is this still done automatically?
	// What happens if we have small differences in facilities->links mapping during simulation?
	private void mapActivities2Facilities(PopulationImpl population) {
		log.info("\tMapping facilities .........................");
		TreeMap<String, QuadTree<ActivityFacility>> trees = new TreeMap<String, QuadTree<ActivityFacility>>();
		
		for (Person p : population.getPersons().values()){
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						ActivityImpl act = (ActivityImpl)pe;
						
						BuildTrees util = new BuildTrees();
						if (trees.get(act.getType()) == null) {
							if (act.getType().equals("work")) {
								String [] workTypes = {"work_sector2", "work_sector3"};
								trees.put("work", util.createActivitiesTree(workTypes, "work", this.scenario));
							}
							else {
								trees.put(act.getType(), util.createActivitiesTree(act.getType(), this.scenario));
							}
						}
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
		int newPopulationSize = (int)(population.getPersons().size() * percent / 100.0);
		log.info("\tSampling plans " + percent + " percent: new population size: " + newPopulationSize + "...............................");
		
		int counter = 0;
		int nextMsg = 1;
		while (population.getPersons().size() > newPopulationSize) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			Random random = new Random();
			int index = random.nextInt(population.getPersons().size());
			Id id = (Id) population.getPersons().keySet().toArray()[index];
			population.getPersons().remove(id);
		}
	}
	
	// at the moment only necessary for cross-border traffic
	private List<Id> dilutedZH(Population population) {
		log.info("\tCutting scenario ...................................");
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
	
	// need to add start and end times
	private void convertFromV1toV2(Scenario inScenario) {		
		for (Person p : inScenario.getPopulation().getPersons().values()){
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						ActivityImpl act = (ActivityImpl)pe;
						String v2Type = ActTypeConverter.convert2FullType(act.getType());
						double duration = 12.0 * 3600.0;
						if (!act.getType().equals("tta")) {
							duration = Double.parseDouble(act.getType().substring(1));
						}
						act.setType(v2Type);
						((PersonImpl)p).createDesires(v2Type);
						((PersonImpl)p).getDesires().putActivityDuration(v2Type, duration);
					}
				}
			}
		}
	}
		
	private void write() {
		new File(this.outputFolder).mkdirs();
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.outputFolder + "plans.xml.gz");
	}
}
