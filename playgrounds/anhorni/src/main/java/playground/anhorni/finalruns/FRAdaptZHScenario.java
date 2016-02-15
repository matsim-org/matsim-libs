/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.finalruns;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.facilities.OpeningTime.DayType;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class FRAdaptZHScenario {
	private final static Logger log = Logger.getLogger(FRAdaptZHScenario.class);
	private Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String outputFolder;
	private String plansFilePath;
	private String networkFilePath;
	private String facilitiesFilePath;
	private double sampleRate = 1.0;
	
	private Coord center = new Coord((double) 682756, (double) 248732); // Letten

	private double radius = 5000.0;
			
	public static void main(final String[] args) {		
		FRAdaptZHScenario plansCreator = new FRAdaptZHScenario();
		
		 String plansFilePath = args[0]; 
		 String networkFilePath = args[1];
		 String facilitiesFilePath = args[2];
		 String outputFolder = args[3];
		 String sampleRateStr = args[4];
		 String bzFile = args[5];
		 String csFile = args[6];
		
		plansCreator.run(plansFilePath, networkFilePath, facilitiesFilePath, outputFolder, sampleRateStr, bzFile, csFile);			
		log.info("Adaptation finished -----------------------------------------");
	}
		
	private void init() {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesFilePath);
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}
	
	private void adaptOpenTimes() {
		for (ActivityFacility fac:this.scenario.getActivityFacilities().getFacilities().values()) {
			if (fac.getActivityOptions().get("h") != null) {
				ActivityOptionImpl actOpt = (ActivityOptionImpl) fac.getActivityOptions().get("h");
				actOpt.getOpeningTimes().clear();
				actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wkday, 0.0, 24.0 * 3600.0));
			}
		}
	}

	public void run(final String plansFilePath, final String networkFilePath, final String facilitiesFilePath, final String outputFolder,
			final String sampleRateStr, final String bzFile, final String csFile) {
		this.plansFilePath = plansFilePath;
		this.networkFilePath = networkFilePath;
		this.facilitiesFilePath = facilitiesFilePath;
		this.outputFolder = outputFolder;
		this.sampleRate = Double.parseDouble(sampleRateStr);
		
		this.init();
				
		log.info("Sample fraction: " + this.sampleRate);
		if (this.sampleRate < 100.0) {
			this.samplePlans(this.sampleRate);
		}	
		log.info("Population size :" + this.scenario.getPopulation().getPersons().size());
		
		log.info("Remove border crossers");
		this.removeBoderCrossers();
		log.info("Population size :" + this.scenario.getPopulation().getPersons().size());
		
//		log.info("Insert sg acts");
//		this.insertSG();
				
		log.info("Add Facilities");
		this.addfacilities2Plans();
		
		log.info("Clean routes");
		this.cleanRoutes();
		
		this.adaptOpenTimes();
						
		this.write();
		
		AddAttributes adapter = new AddAttributes();
		adapter.run(plansFilePath, networkFilePath, facilitiesFilePath, outputFolder, bzFile, csFile);
	}
	
	private void writeBetas() {
		ObjectAttributes betas = new ObjectAttributes();
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {	
			betas.putAttribute(p.getId().toString(), "size", 1.0);
			betas.putAttribute(p.getId().toString(), "price", -1.0);
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(betas);
		betaWriter.writeFile(outputFolder + "/betas.xml");
	}
	
	private void cleanRoutes() {
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {				
			Plan plan = p.getSelectedPlan();
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {
					LegImpl leg = (LegImpl)pe;
					leg.setRoute(null);
				}
			}
		}
	}
	
//	private void insertSG() {
//		Random random = new Random(37835409);
//		int counter = 0;
//		int nextMsg = 1;
//		for (Person p : this.scenario.getPopulation().getPersons().values()) {				
//			Plan plan = p.getSelectedPlan();
//			counter++;
//			if (counter % nextMsg == 0) {
//				nextMsg *= 2;
//				log.info(" person # " + counter);
//			}
//			for (PlanElement pe : plan.getPlanElements()) {
//				if (pe instanceof Activity) {
//					ActivityImpl act = (ActivityImpl)pe;					
//					if (act.getType().startsWith("s")) {
//						if (random.nextFloat() < 0.725) {
//							String duration = act.getType().substring(1);
//							act.setType("sg" + duration);
//						}
//					}
//				}
//			}
//		}
//	}
	
	private void removeBoderCrossers() {
		List<Person> personsWithoutCB = new Vector<Person>();
		for (Person p : this.scenario.getPopulation().getPersons().values()) {				
			if (Integer.parseInt(p.getId().toString()) < 1000000000) {
				personsWithoutCB.add(p);
			}
		}
		this.scenario.getPopulation().getPersons().clear();
		for (Person p: personsWithoutCB) {
			this.scenario.getPopulation().addPerson(p);
		}
	}
	
	private void addfacilities2Plans() {			
		TreeMap<String, QuadTree<ActivityFacility>> trees = new TreeMap<String, QuadTree<ActivityFacility>>();
		trees.put("h", this.builFacQuadTree("h", this.scenario.getActivityFacilities().getFacilitiesForActivityType("h")));
		trees.put("w", this.builFacQuadTree("w", this.scenario.getActivityFacilities().getFacilitiesForActivityType("w")));
		trees.put("e", this.builFacQuadTree("e", this.scenario.getActivityFacilities().getFacilitiesForActivityType("e")));
		trees.put("s", this.builFacQuadTree("s", this.scenario.getActivityFacilities().getFacilitiesForActivityType("s")));
//		trees.put("sg", this.builFacQuadTree("sg", this.scenario.getActivityFacilities().getFacilitiesForActivityType("sg")));
		trees.put("l", this.builFacQuadTree("l", this.scenario.getActivityFacilities().getFacilitiesForActivityType("l")));
				
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {				
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
					
					// inner city assign sg and s
//					double dist = CoordUtils.calcDistance(act.getCoord(), center);
//					if (dist < radius) {
//						if (act.getType().startsWith("sg")) {
//							((ActivityImpl)pe).setFacilityId(trees.get("sg").get(act.getCoord().getX(), act.getCoord().getY()).getId());
//						}
//						else {
//							((ActivityImpl)pe).setFacilityId(trees.get("s").get(act.getCoord().getX(), act.getCoord().getY()).getId());
//						}
//					}
//					else {
//						if (act.getType().startsWith("sg")) {
//							((ActivityImpl)pe).setFacilityId(trees.get("s").get(act.getCoord().getX(), act.getCoord().getY()).getId());
//						}
//						else {
							((ActivityImpl)pe).setFacilityId(
									trees.get(act.getType().substring(0, 1)).
											getClosest(act.getCoord().getX(), act.getCoord().getY()).
									getId());					
//						}
//					}
				}
			}
		}
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
		
		this.writeBetas();
	}
}
