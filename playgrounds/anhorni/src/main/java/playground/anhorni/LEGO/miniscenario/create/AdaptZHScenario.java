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

package playground.anhorni.LEGO.miniscenario.create;

import java.io.File;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;

public class AdaptZHScenario {
	private final static Logger log = Logger.getLogger(AdaptZHScenario.class);
	private Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String outputFolder;
			
	public static void main(final String[] args) {		
		AdaptZHScenario plansCreator = new AdaptZHScenario();		
		plansCreator.run(args[0]);			
		log.info("Adaptation finished -----------------------------------------");
	}
		
	private void init(final String plansFilePath, final String networkFilePath, final String facilitiesFilePath) {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}

	public void run(String configFile) {			
//		Config config = (ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(configFile).getScenario()).getConfig();

		// the following is not an equivalent translation, but I don't think that scenario was needed at all:
		Config config = ConfigUtils.loadConfig(configFile);
		MatsimRandom.reset(config.global().getRandomSeed());
		// (probably not needed, but was in originally called code. kai, oct'13)

		this.init(config.getModule("plans").getValue("inputPlansFile"), 
				config.getModule("network").getValue("inputNetworkFile"), 
				config.getModule("facilities").getValue("inputFacilitiesFile"));
		
		this.outputFolder = config.getModule("controler").getValue("outputDirectory");
		
		double sampleFraction = 100.0 / Double.parseDouble(config.getModule("counts").getValue("countsScaleFactor"));
		log.info("Sample fraction: " + sampleFraction);
		if (sampleFraction < 100.0) {
			this.samplePlans(sampleFraction);
		}
				
		log.info("Adding opening times to facilities ...");
		this.addOpeningTimes();
		log.info("Adapting plans ... of " + this.scenario.getPopulation().getPersons().size() + " persons");
		this.addfacilities2Plans();
		
		this.write();
	}
	
	private void addfacilities2Plans() {	
		
//		Vector<PersonImpl> personsWithoutCB = new Vector<PersonImpl>();
		
		TreeMap<String, QuadTree<ActivityFacility>> trees = new TreeMap<String, QuadTree<ActivityFacility>>();
		trees.put("home", this.builFacQuadTree("home", this.scenario.getActivityFacilities().getFacilitiesForActivityType("h")));
		trees.put("work", this.builFacQuadTree("work", this.scenario.getActivityFacilities().getFacilitiesForActivityType("w")));
		trees.put("education", this.builFacQuadTree("education", this.scenario.getActivityFacilities().getFacilitiesForActivityType("e")));
		trees.put("shop", this.builFacQuadTree("shop", this.scenario.getActivityFacilities().getFacilitiesForActivityType("s")));
		trees.put("leisure", this.builFacQuadTree("leisure", this.scenario.getActivityFacilities().getFacilitiesForActivityType("l")));
		trees.put("tta", this.builFacQuadTree("tta", this.scenario.getActivityFacilities().getFacilitiesForActivityType("tta")));
		
//		ActTypeConverter actTypeConverter = new ActTypeConverter(false);
		
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {	
			
//			if (Integer.parseInt(p.getId().toString()) < 1000000000) {
//				personsWithoutCB.add((PersonImpl)p);
//			}
			Plan plan = p.getSelectedPlan();
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					ActivityImpl act = (ActivityImpl)pe;
					
					if (act.getEndTime()== Time.UNDEFINED_TIME) {
						Leg previousLeg = ((PlanImpl)plan).getPreviousLeg(act);
						Activity previousAct = ((PlanImpl)plan).getPreviousActivity(previousLeg);
						double endTime = previousAct.getEndTime() + previousLeg.getTravelTime() + act.getMaximumDuration();
						act.setEndTime(endTime);
					}
										
//					double duration = 24.0 * 3600.0;
//					if (!act.getType().equals("tta")) {
//						duration = 3600 * Double.parseDouble(act.getType().substring(1));
//					}					
//					((PersonImpl)p).getDesires().putActivityDuration(fullType, duration);
//					act.setType(fullType);
					if (act.getType().equals("tta")) {
						((ActivityImpl)pe).setFacilityId(
								trees.get("tta").getClosest(act.getCoord().getX(), act.getCoord().getY()).
								getId());
					}
					else {
						log.error("correct for type conversion");
						System.exit(-99);
						
						
//						((ActivityImpl)pe).setFacilityId(
//								trees.get(ActTypeConverter.convert2FullType(act.getType())).get(act.getCoord().getX(), act.getCoord().getY())
//								.getId());
					}
				}
			}
		}
//		this.scenario.getPopulation().getPersons().clear();
//		for (Person p: personsWithoutCB) {
//			this.scenario.getPopulation().addPerson(p);
//		}
	}
	
	private void addOpeningTimes() {
		for (ActivityFacility facility : this.scenario.getActivityFacilities().getFacilities().values()) {
			
			Vector<ActivityOption> options = new Vector<ActivityOption>();

			for (ActivityOption option : facility.getActivityOptions().values()) {				
				if (option.getType().startsWith("h")) {
					options.add(this.replaceActOption("h", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("w")) {
					options.add(this.replaceActOption("w", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("e")) {
					options.add(this.replaceActOption("e", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("s")) {
					options.add(this.replaceActOption("s", (ActivityOptionImpl)option, facility));
					options.add(this.replaceActOption("shop", (ActivityOptionImpl)option, facility));
				}
				else if (option.getType().startsWith("l")) { 
					options.add(this.replaceActOption("l", (ActivityOptionImpl)option, facility));
					options.add(this.replaceActOption("leisure", (ActivityOptionImpl)option, facility));
				}
				else {
					options.add(this.replaceActOption("tta", (ActivityOptionImpl)option, facility));
				}
				
			}
			facility.getActivityOptions().clear();
			for (ActivityOption option : options) {
				facility.getActivityOptions().put(option.getType(), option);
			}
		}	
	}
	
	private ActivityOptionImpl replaceActOption(String type, ActivityOptionImpl option, ActivityFacility facility) {
		ActivityOptionImpl optionNew = new ActivityOptionImpl(type);
		optionNew.setFacility(facility);
				
		for (OpeningTime ot : option.getOpeningTimes()) {
			optionNew.addOpeningTime(ot);
		}
		optionNew.setCapacity(option.getCapacity());
		return optionNew;
	}
					
	private QuadTree<ActivityFacility> builFacQuadTree(String type, TreeMap<Id<ActivityFacility>, ActivityFacility> facilities_of_type) {
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
	
	private void samplePlans(double percent) {
		int newPopulationSize = (int)(this.scenario.getPopulation().getPersons().size() * percent / 100.0);
		log.info("\tSampling plans " + percent + " percent: new population size: " + newPopulationSize + "...............................");
		
		int counter = 0;
		int nextMsg = 1;
		while (this.scenario.getPopulation().getPersons().size() > newPopulationSize) {
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			Random random = new Random(37835409);
			int index = random.nextInt(this.scenario.getPopulation().getPersons().size());
			Id id = (Id) this.scenario.getPopulation().getPersons().keySet().toArray()[index];
			this.scenario.getPopulation().getPersons().remove(id);
		}
	}
	
	private void write() {
		new File(this.outputFolder).mkdirs();
		new FacilitiesWriter(this.scenario.getActivityFacilities()).write(this.outputFolder + "facilities.xml.gz");
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.outputFolder + "plans.xml.gz");
	}
}
