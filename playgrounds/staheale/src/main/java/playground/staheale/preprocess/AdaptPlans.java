/* *********************************************************************** *
 * project: org.matsim.*
 * AdaptPlans.java
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

package playground.staheale.preprocess;

import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.network.Network;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;


public class AdaptPlans {
	private static Logger log = Logger.getLogger(AdaptPlans.class);
	private ScenarioImpl scenario;
	private ScenarioImpl scenarioNew;
	private QuadTree<ActivityFacility> sportsFunQuadTree;
	private QuadTree<ActivityFacility> workQuadTree;
	private QuadTree<ActivityFacility> gastroCultureQuadTree;
	private QuadTree<ActivityFacility> educationQuadTree;
	private QuadTree<ActivityFacility> shopRetailQuadTree;
	private QuadTree<ActivityFacility> shopServiceQuadTree;
	boolean tta = false;

public AdaptPlans() {
	super();		
}
	
public static void main(String[] args) throws IOException {
	AdaptPlans adaptPlans = new AdaptPlans();
	adaptPlans.run();
	}

public void run() {
	scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
    log.info("Reading network xml file...");
    MatsimNetworkReader NetworkReader = new MatsimNetworkReader(scenario);
	NetworkReader.readFile("./input/network.xml");
	Network network = scenario.getNetwork();
    log.info("Reading network xml file...done.");
    log.info("Number of nodes: " +network.getNodes().size());
    log.info("Number of links: " +network.getLinks().size());
	
	MatsimPopulationReader PlansReader = new MatsimPopulationReader(scenario); 
	PlansReader.readFile("./input/plans.xml.gz");
	
	MatsimFacilitiesReader FacReader = new MatsimFacilitiesReader((ScenarioImpl) scenario);  
	System.out.println("Reading facilities xml file... ");
	FacReader.readFile("./input/facilities.xml.gz");
	System.out.println("Reading facilities xml file...done.");
	ActivityFacilitiesImpl facilities = ((ScenarioImpl) scenario).getActivityFacilities();
    log.info("Number of facilities: " +facilities.getFacilities().size());
    
	scenarioNew = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

    MatsimNetworkReader NetworkReader2 = new MatsimNetworkReader(scenarioNew);
	NetworkReader2.readFile("./input/network.xml");
	
	MatsimPopulationReader PlansReader2 = new MatsimPopulationReader(scenarioNew); 
	PlansReader2.readFile("./input/plans0.xml");
	
	MatsimFacilitiesReader FacReader2 = new MatsimFacilitiesReader((ScenarioImpl) scenarioNew);  
	FacReader2.readFile("./input/facilities.xml.gz");

    
//	Random random = new Random(4711);

//	for (Person p : scenario.getPopulation().getPersons().values()) {
//		for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
//			if (pe instanceof Activity) {
//				((PersonImpl)p).createDesires("desired activity durations");
//                ActivityImpl act = (ActivityImpl)pe;                         
//                if (act.getType().startsWith("s")) {
//                	if (random.nextDouble()<0.95){
//                		String OLD_ACT_TYPE = act.getType();
//    					String DE = OLD_ACT_TYPE.substring(1);
//    					double desire = Double.parseDouble(DE)*3600;
//                    	((PersonImpl)p).getDesires().putActivityDuration("shop_retail", desire);
//                		act.setType("shop_retail");
//                	}
//                	else {
//                		String OLD_ACT_TYPE = act.getType();
//    					String DE = OLD_ACT_TYPE.substring(1);
//    					double desire = Double.parseDouble(DE)*3600;
//                    	((PersonImpl)p).getDesires().putActivityDuration("shop_service", desire);
//                		act.setType("shop_service");
//                	}
//                }
//                if (act.getType().startsWith("l")) {
//                	if (random.nextDouble()<0.5){
//                		String OLD_ACT_TYPE = act.getType();
//    					String DE = OLD_ACT_TYPE.substring(1);
//    					double desire = Double.parseDouble(DE)*3600;
//                    	((PersonImpl)p).getDesires().putActivityDuration("sports_fun", desire);
//                		act.setType("sports_fun");
//                	}
//                	else {
//                		String OLD_ACT_TYPE = act.getType();
//    					String DE = OLD_ACT_TYPE.substring(1);
//    					double desire = Double.parseDouble(DE)*3600;
//                    	((PersonImpl)p).getDesires().putActivityDuration("gastro_culture", desire);
//                		act.setType("gastro_culture");
//                	}
//                }
//                if (act.getType().startsWith("w")) {
//                	String OLD_ACT_TYPE = act.getType();
//					String DE = OLD_ACT_TYPE.substring(1);
//					double desire = Double.parseDouble(DE)*3600;
//                	((PersonImpl)p).getDesires().putActivityDuration("work", desire);
//                	act.setType("work");
//                }
//                if (act.getType().startsWith("h")) {
//                	String OLD_ACT_TYPE = act.getType();
//					String DE = OLD_ACT_TYPE.substring(1);
//					double desire = Double.parseDouble(DE)*3600;
//                	((PersonImpl)p).getDesires().putActivityDuration("home", desire);
//                	act.setType("home");
//                }
//                if (act.getType().startsWith("e")) {
//                	String OLD_ACT_TYPE = act.getType();
//					String DE = OLD_ACT_TYPE.substring(1);
//					double desire = Double.parseDouble(DE)*3600;
//                	((PersonImpl)p).getDesires().putActivityDuration("education", desire);
//                	act.setType("education");
//                }
//			}
//		}
//	}

	//---------------------adapt activity facility to newly generated facilities
	
//	TreeMap<Id,ActivityFacility> shopRetailFacilities = facilities.getFacilitiesForActivityType("shop_retail");
//	log.info("Shop retail facilities: " +shopRetailFacilities.size());
//	TreeMap<Id,ActivityFacility> shopServiceFacilities = facilities.getFacilitiesForActivityType("shop_service");
//	log.info("Shop service facilities: " +shopServiceFacilities.size());
//	TreeMap<Id,ActivityFacility> sportsFunFacilities = facilities.getFacilitiesForActivityType("sports_fun");
//	log.info("Sports & fun facilities: " +sportsFunFacilities.size());
	
	
//	TreeMap<Id,ActivityFacility> workFacilities = facilities.getFacilitiesForActivityType("work_sector2");
//	TreeMap<Id,ActivityFacility> work3Facilities = facilities.getFacilitiesForActivityType("work_sector3");
//	for (ActivityFacility f : work3Facilities.values()){
//		workFacilities.put(f.getId(), f);
//	}
//	log.info("Work facilities: " +workFacilities.size());
	
	
//	TreeMap<Id,ActivityFacility> gastroCultureFacilities = facilities.getFacilitiesForActivityType("gastro_culture");
//	log.info("Gastro & culture facilities: " +gastroCultureFacilities.size());
//	TreeMap<Id,ActivityFacility> educationFacilities = facilities.getFacilitiesForActivityType("education");
//	log.info("Education facilities: " +educationFacilities.size());
//
//	shopRetailQuadTree = this.buildShopRetailQuadTree(shopRetailFacilities);
//	log.info(" shopRetailQuadTree size: " +this.shopRetailQuadTree.size());
//	
//	shopServiceQuadTree = this.buildShopServiceQuadTree(shopServiceFacilities);
//	log.info(" shopServiceQuadTree size: " +this.shopServiceQuadTree.size());
//
//	sportsFunQuadTree = this.buildSportsFunQuadTree(sportsFunFacilities);
//	log.info(" sportsFunQuadTree size: " +this.sportsFunQuadTree.size());

//	workQuadTree = this.buildWorkQuadTree(workFacilities);
//	log.info(" workQuadTree size: " +this.workQuadTree.size());
	
//	gastroCultureQuadTree = this.buildGastroCultureQuadTree(gastroCultureFacilities);
//	log.info(" gastroCultureQuadTree size: " +this.gastroCultureQuadTree.size());

//	educationQuadTree = this.buildEducationQuadTree(educationFacilities);
//	log.info(" educationQuadTree size: " +this.educationQuadTree.size());

	for (Person p : scenario.getPopulation().getPersons().values()) {
		for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
			if (pe instanceof Activity) {
				ActivityImpl act = (ActivityImpl)pe; 
				if (!act.getType().startsWith("tta") && tta == false) {
					tta = true;
					log.info("tta set true for person "+p.getId());
					scenarioNew.getPopulation().addPerson(p);
					log.info("person " +p.getId()+ " added to population");
				}
            }
		}
		tta = false;

//		if (tta == false){
//			log.info("tta false for person "+p.getId());
//			scenarioNew.getPopulation().addPerson(p);
//		}
	}
		
//	for (Person p : scenario.getPopulation().getPersons().values()) {
//		for (PlanElement pe : p.getSelectedPlan().getPlanElements()){
//			if (pe instanceof Activity) {
//                ActivityImpl act = (ActivityImpl)pe; 
//                if (act.getType().startsWith("shop_re")) {
//        			Double x = act.getCoord().getX();
//        			Double y = act.getCoord().getY();
//        			ActivityFacility closestShopRetail = shopRetailQuadTree.get(x,y);
//        			act.setFacilityId(closestShopRetail.getId());
//        			act.setCoord(closestShopRetail.getCoord()); 
//                }
//                if (act.getType().startsWith("shop_ser")) {
//        			Double x = act.getCoord().getX();
//        			Double y = act.getCoord().getY();
//        			ActivityFacility closestShopService = shopServiceQuadTree.get(x,y);
//        			act.setFacilityId(closestShopService.getId());
//        			act.setCoord(closestShopService.getCoord()); 
//                }
//                if (act.getType().startsWith("sports")) {
//        			Double x = act.getCoord().getX();
//        			Double y = act.getCoord().getY();
//        			ActivityFacility closestSportsFun = sportsFunQuadTree.get(x,y);
//        			act.setFacilityId(closestSportsFun.getId());
//        			act.setCoord(closestSportsFun.getCoord()); 
//                }
////                if (act.getType().startsWith("wor")) {
////        			Double x = act.getCoord().getX();
////        			Double y = act.getCoord().getY();
////        			ActivityFacility closestWork = workQuadTree.get(x,y);
////        			act.setFacilityId(closestWork.getId());
////        			act.setCoord(closestWork.getCoord()); 
////                }
//                if (act.getType().startsWith("gastro")) {
//        			Double x = act.getCoord().getX();
//        			Double y = act.getCoord().getY();
//        			ActivityFacility closestGastroCulture = gastroCultureQuadTree.get(x,y);
//        			act.setFacilityId(closestGastroCulture.getId());
//        			act.setCoord(closestGastroCulture.getCoord()); 
//                }
////                if (act.getType().startsWith("edu")) {
////        			Double x = act.getCoord().getX();
////        			Double y = act.getCoord().getY();
////        			ActivityFacility closestEducation = educationQuadTree.get(x,y);
////        			act.setFacilityId(closestEducation.getId());
////        			act.setCoord(closestEducation.getCoord()); 
////                }
//			}
//		}
//	}
	new PopulationWriter(scenarioNew.getPopulation(), scenarioNew.getNetwork()).write("./output/plans.xml.gz");	

	}

private QuadTree<ActivityFacility> buildSportsFunQuadTree(TreeMap<Id,ActivityFacility> sportsFunFacilities) {
	double minx = Double.POSITIVE_INFINITY;
	double miny = Double.POSITIVE_INFINITY;
	double maxx = Double.NEGATIVE_INFINITY;
	double maxy = Double.NEGATIVE_INFINITY;
	
	for (final ActivityFacility f : sportsFunFacilities.values()) {
		if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
		if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
		if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
		if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
	}
	minx -= 1.0;
	miny -= 1.0;
	maxx += 1.0;
	maxy += 1.0;
	log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");

	QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
	for (final ActivityFacility f : sportsFunFacilities.values()) {
		quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacility) f);
		}
	return quadtree;
	}

//private QuadTree<ActivityFacility> buildWorkQuadTree(TreeMap<Id,ActivityFacility> workFacilities) {
//	double minx = Double.POSITIVE_INFINITY;
//	double miny = Double.POSITIVE_INFINITY;
//	double maxx = Double.NEGATIVE_INFINITY;
//	double maxy = Double.NEGATIVE_INFINITY;
//	
//	for (final ActivityFacility f : workFacilities.values()) {
//		if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
//		if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
//		if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
//		if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
//	}
//	minx -= 1.0;
//	miny -= 1.0;
//	maxx += 1.0;
//	maxy += 1.0;
//	log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
//
//	QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
//	for (final ActivityFacility f : workFacilities.values()) {
//		quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacility) f);
//		}
//	return quadtree;
//	}

private QuadTree<ActivityFacility> buildGastroCultureQuadTree(TreeMap<Id,ActivityFacility> gastroCultureFacilities) {
	double minx = Double.POSITIVE_INFINITY;
	double miny = Double.POSITIVE_INFINITY;
	double maxx = Double.NEGATIVE_INFINITY;
	double maxy = Double.NEGATIVE_INFINITY;
	
	for (final ActivityFacility f : gastroCultureFacilities.values()) {
		if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
		if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
		if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
		if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
	}
	minx -= 1.0;
	miny -= 1.0;
	maxx += 1.0;
	maxy += 1.0;
	log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");

	QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
	for (final ActivityFacility f : gastroCultureFacilities.values()) {
		quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacility) f);
		}
	return quadtree;
	}

//private QuadTree<ActivityFacility> buildEducationQuadTree(TreeMap<Id,ActivityFacility> educationFacilities) {
//	double minx = Double.POSITIVE_INFINITY;
//	double miny = Double.POSITIVE_INFINITY;
//	double maxx = Double.NEGATIVE_INFINITY;
//	double maxy = Double.NEGATIVE_INFINITY;
//	
//	for (final ActivityFacility f : educationFacilities.values()) {
//		if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
//		if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
//		if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
//		if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
//	}
//	minx -= 1.0;
//	miny -= 1.0;
//	maxx += 1.0;
//	maxy += 1.0;
//	log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
//
//	QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
//	for (final ActivityFacility f : educationFacilities.values()) {
//		quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacility) f);
//		}
//	return quadtree;
//	}
private QuadTree<ActivityFacility> buildShopRetailQuadTree(TreeMap<Id,ActivityFacility> shopRetailFacilities) {
	double minx = Double.POSITIVE_INFINITY;
	double miny = Double.POSITIVE_INFINITY;
	double maxx = Double.NEGATIVE_INFINITY;
	double maxy = Double.NEGATIVE_INFINITY;
	
	for (final ActivityFacility f : shopRetailFacilities.values()) {
		if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
		if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
		if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
		if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
	}
	minx -= 1.0;
	miny -= 1.0;
	maxx += 1.0;
	maxy += 1.0;
	log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");

	QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
	for (final ActivityFacility f : shopRetailFacilities.values()) {
		quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacility) f);
		}
	return quadtree;
	}
private QuadTree<ActivityFacility> buildShopServiceQuadTree(TreeMap<Id,ActivityFacility> shopServiceFacilities) {
	double minx = Double.POSITIVE_INFINITY;
	double miny = Double.POSITIVE_INFINITY;
	double maxx = Double.NEGATIVE_INFINITY;
	double maxy = Double.NEGATIVE_INFINITY;
	
	for (final ActivityFacility f : shopServiceFacilities.values()) {
		if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
		if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
		if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
		if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
	}
	minx -= 1.0;
	miny -= 1.0;
	maxx += 1.0;
	maxy += 1.0;
	log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");

	QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
	for (final ActivityFacility f : shopServiceFacilities.values()) {
		quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacility) f);
		}
	return quadtree;
	}
}