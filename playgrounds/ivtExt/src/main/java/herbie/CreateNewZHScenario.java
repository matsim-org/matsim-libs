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

package herbie;

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
import org.matsim.core.gbl.Gbl;
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

public class CreateNewZHScenario {
	private String currentDir = "//pingelap/matsim/";
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
	
	private double sampleFraction = 100.0;
	
	// ====================================================================================
	public static void main(final String[] args) {
		if (args.length < 2) {
			log.error("Please specify a running location! Either 'l' (locally) or 'r' (remotely) and please specify a sample rate in percent");
			return;
		}				
		CreateNewZHScenario creator = new CreateNewZHScenario();
		creator.init();
		creator.run(args[0], Double.parseDouble(args[1]));
		log.info("Creation finished -----------------------------------------");
	}
	
	// ====================================================================================
	private void run(String runningLocation, double sampleFraction) {
		if (runningLocation.equals("l")) {
			this.currentDir = "//pingelap/matsim/";
		}
		else {
			this.currentDir = "/Network/Servers/pingelap/Volumes/ivt-shared/Groups/ivt/vpl/projekt/matsim/";
		}
		this.sampleFraction = sampleFraction;
		this.init();
		
		this.addSpecialPlans2Population(this.crossBorderPlansFilePath, "cross-border");
		this.addSpecialPlans2Population(this.freightPlansFilePath, "freight");
		this.write();
	}
	
	// ====================================================================================
	// read in network, facilities and plans into scenario
	private void init() {
		this.readPathsFile(currentDir, "herbie/configs/paths-config.xml");
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansV2filePath);
	}
	
	private void readPathsFile(String currentDir, String pathsfile) {
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(currentDir + pathsfile);   	
		
		this.outputFolder = currentDir + config.getParam("pathsettings", "outputFolder");
		
		// old scenario parts -----
		this.networkfilePath = currentDir + config.getParam("pathsettings", "networkfilePath");
		this.facilitiesfilePath = currentDir + config.getParam("pathsettings", "facilitiesfilePath");
		this.plansV2filePath = currentDir + config.getParam("pathsettings", "plansV2filePath");
		
		// new demand -------------
		this.crossBorderPlansFilePath = currentDir + config.getParam("pathsettings", "crossBorderPlansFilePath");
		this.freightPlansFilePath = currentDir + config.getParam("pathsettings", "freightPlansFilePath");
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
			this.mapActivities2Facilities((PopulationImpl) sTmp.getPopulation());
			List<Id> persons2remove = this.dilutedZH(sTmp.getPopulation());
			for (Id personId : persons2remove) {
				sTmp.getPopulation().getPersons().remove(personId);
			}
		}
		
		// facilities are already mapped
		if (type.equals("freight")) {
			List<Id> persons2remove = this.dilutedZH(sTmp.getPopulation());
			for (Id personId : persons2remove) {
				sTmp.getPopulation().getPersons().remove(personId);
			}
		}
		this.samplePlans(sTmp.getPopulation(), this.sampleFraction);
		log.info("Remaining " + type + " agents" + sTmp.getPopulation().getPersons().size());
		
		for (Person p : sTmp.getPopulation().getPersons().values()){
			this.scenario.getPopulation().addPerson(p);
		}
		log.info("Number of agents in new scenario: " + this.scenario.getPopulation().getPersons().size());
	} 
	
	//TODO: Is this really necessary?
	// What happens if we have small differences in facilities->links mapping during simulation?
	private void mapActivities2Facilities(PopulationImpl population) {
		TreeMap<String, QuadTree<ActivityFacility>> trees = new TreeMap<String, QuadTree<ActivityFacility>>();
		
		for (Person p : population.getPersons().values()){
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						ActivityImpl act = (ActivityImpl)pe;
						if (trees.get(act.getType()) == null) trees.put(act.getType(), this.createActivitiesTree(act.getType()));
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
		int newPopulationSize = (int)(population.getPersons().size() * percent);
		
		while (population.getPersons().size() > newPopulationSize) {
			Random random = new Random();
			int index = random.nextInt(population.getPersons().size());
			population.getPersons().remove(new IdImpl(index));
		}
	}
	
	// at the moment only necessary for cross-border traffic
	private List<Id> dilutedZH(Population population) {
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
	
	private QuadTree<ActivityFacility> createActivitiesTree(String activityType) {
		QuadTree<ActivityFacility> facQuadTree = this.builFacQuadTree(
				activityType, this.scenario.getActivityFacilities().getFacilitiesForActivityType(activityType));			
		return facQuadTree;
	}
	
	private QuadTree<ActivityFacility> builFacQuadTree(String type, TreeMap<Id,ActivityFacility> facilities_of_type) {
		Gbl.startMeasurement();
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : facilities_of_type.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("Quadtree size: " + quadtree.size());
		return quadtree;
	}
}
