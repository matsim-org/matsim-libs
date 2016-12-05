/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityLocationAdjuster.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.southafrica.projects.erAfrica;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.households.Household;

import playground.southafrica.population.utilities.ComprehensivePopulationReader;
import playground.southafrica.utilities.Header;

/**
 * This class reads in the population with its allocated activity chains, and 
 * adjusts each activity's location by considering the leg's distance, and the
 * location of actual parsed activity facilities (OpenStreetMap, Cape Town 
 * landuse, or Cape Town's informal settlements).
 *  
 * @author jwjoubert
 */
public class ERAfricaActivityLocationAdjuster {
	final private static Logger LOG = Logger.getLogger(ERAfricaActivityLocationAdjuster.class);
	private Scenario sc;
	private QuadTree<ActivityFacility> qtHomeFormal;
	private QuadTree<ActivityFacility> qtHomeInformal;
	private QuadTree<ActivityFacility> qtEducation;
	private QuadTree<ActivityFacility> qtWork;
	private QuadTree<ActivityFacility> qtShopping;
	private QuadTree<ActivityFacility> qtLeisure;
	private QuadTree<ActivityFacility> qtMedical;
	private QuadTree<ActivityFacility> qtOther;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ERAfricaActivityLocationAdjuster.class.toString(), args);

		String populationFolder = args[0];
		String osmFacilitiesFile = args[1];
		String ctFacilitiesFile = args[2];
		String ctInformalHousing = args[3];
		String finalPopulation = args[4];

		ComprehensivePopulationReader cpr = new ComprehensivePopulationReader();
		cpr.parse(populationFolder);
		Scenario sc = cpr.getScenario();

		ERAfricaActivityLocationAdjuster ala = new ERAfricaActivityLocationAdjuster(sc);
		ala.buildQuadTreeFromFacilities(osmFacilitiesFile, ctFacilitiesFile, ctInformalHousing);
		ala.processHouseholds();

		Header.printFooter();
	}

	public ERAfricaActivityLocationAdjuster(Scenario sc) {
		this.sc = sc;
	}


	public void buildQuadTreeFromFacilities(String osmFile, String ctLanduseFile, String ctInformalFile){
		LOG.info("Building the necessary QuadTree objects...");
		Scenario thisSc = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		/* Determine the extent of the QuadTree. */
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;

		new FacilitiesReaderMatsimV1(thisSc).readFile(osmFile);
		new FacilitiesReaderMatsimV1(thisSc).readFile(ctLanduseFile);
		new FacilitiesReaderMatsimV1(thisSc).readFile(ctInformalFile);
		for(ActivityFacility facility : thisSc.getActivityFacilities().getFacilities().values()){
			Coord c = facility.getCoord();
			xMin = Math.min(xMin, c.getX());
			xMax = Math.max(xMax, c.getX());
			yMin = Math.min(yMin, c.getY());
			yMax = Math.max(yMax, c.getY());
		}
		
		/* Housing. */
		LOG.info("  ... home...");
		qtHomeFormal = new QuadTree<>(xMin, yMin, xMax, yMax);
		qtHomeInformal = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.HOME).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			if(fId.toString().startsWith("gdb")){
				qtHomeFormal.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			} else if(fId.toString().startsWith("inf")){
				qtHomeInformal.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
			}
		}

		/* Education. */
		LOG.info("  ... education...");
		qtEducation = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.EDUCATION).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			qtEducation.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Work. */
		LOG.info("  ... work...");
		qtWork = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.WORK).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			qtWork.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Shopping. */
		LOG.info("  ... shopping...");
		qtShopping = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.SHOPPING).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			qtShopping.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Leisure. */
		LOG.info("  ... leisure...");
		qtLeisure = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.LEISURE).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			qtLeisure.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Medical. */
		LOG.info("  ... medical...");
		qtMedical = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.MEDICAL).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			qtMedical.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.HOSPITAL).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			qtMedical.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		
		/* Other. */
		LOG.info("  ... other...");
		qtOther = new QuadTree<>(xMin, yMin, xMax, yMax);
		for(Id<ActivityFacility> fId : thisSc.getActivityFacilities().getFacilitiesForActivityType(FacilityTypes.OTHER).keySet()){
			ActivityFacility facility = thisSc.getActivityFacilities().getFacilities().get(fId);
			qtOther.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
		}
		LOG.info("Done building QuadTree objects.");
	}



	public void processHouseholds(){
		LOG.info("Handle each member of each household...");
		
		/*FIXME Households' home coordinate were initially hard coded using
		 * WGS84_SA_ALbers as CRS. This should be WGSS84. */
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "EPSG:2048");
		
		Counter counter = new Counter("  households # ");
		for(Household hh : sc.getHouseholds().getHouseholds().values()){
			
			/* Find an appropriate home for the household. */
			Coord censusCoord = ct.transform((Coord) sc.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "homeCoord"));
			String mainDwelling = sc.getHouseholds().getHouseholdAttributes().getAttribute(hh.getId().toString(), "mainDwellingType").toString();
			Coord homeCoord = null;
			if(mainDwelling.equalsIgnoreCase("Informal")){
				ActivityFacility home = sampleSingleActivityFacility(qtHomeInformal, censusCoord);
				homeCoord = home.getCoord();
				
				/* Informal housing units are removed one selected. */
				qtHomeInformal.remove(home.getCoord().getX(), home.getCoord().getY(), home);
			} else{
				ActivityFacility home = sampleSingleActivityFacility(qtHomeFormal, censusCoord);
				homeCoord = home.getCoord();
				
				/* Remove single houses with capacity of 1. */
				if(home.getActivityOptions().get(FacilityTypes.HOME).getCapacity() == 1.0){
					qtHomeFormal.remove(homeCoord.getX(), homeCoord.getY(), home);
				}
			}
			
			
			for(Id<Person> pId : hh.getMemberIds()){
				Plan plan = this.sc.getPopulation().getPersons().get(pId).getSelectedPlan();
				relocatePlanActivities(plan, homeCoord);
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done handling households.");
	}
	
	public ActivityFacility sampleSingleActivityFacility(QuadTree<ActivityFacility> qt, Coord c){
		ActivityFacility facility = null;
		double radius = 1000.0;
		Collection<ActivityFacility> col = qt.getDisk(c.getX(), c.getY(), radius);
		int tries = 1;
		while(col.size() == 0 && tries <= 10){
			radius *= 2;
			col = qt.getDisk(c.getX(), c.getY(), radius);
			tries++;
		}
		List<ActivityFacility> list = new ArrayList<>(col);
		
		if(list.size() > 0){
			facility = list.get(MatsimRandom.getLocalInstance().nextInt(list.size()));
		} else{
			LOG.error("Could not sample a facility.");
		}
		return facility;
	}
	
	
	public void relocatePlanActivities(Plan plan, Coord home){
		List<PlanElement> elements = plan.getPlanElements();
		
		Coord previousLocation = null;
		
		/* Handle first activity, assuming that each plan must start with an activity. */
		if(elements.get(0) instanceof Activity){
			Activity first = (Activity)elements.get(0);
			if(first.getType().startsWith("h")){
				first.setCoord(home);
			} else{
				QuadTree<ActivityFacility> qt = getQuadtreeFromActivityType(first.getType());
			}
		} else{
			throw new IllegalArgumentException("This class assumes that a plan starts with an 'Activity'.");
		}
		
		for(int i = 1; i < elements.size(); i++){
			
		}

	}
	
	
	private QuadTree<ActivityFacility> getQuadtreeFromActivityType(String type){
		QuadTree<ActivityFacility> qt = null;
		
		
		if(qt ==  null){
			LOG.error("Could not find an appropriate QuadTree for activity type ''" + type + "''");
		}
		return qt;
	}
	

}
