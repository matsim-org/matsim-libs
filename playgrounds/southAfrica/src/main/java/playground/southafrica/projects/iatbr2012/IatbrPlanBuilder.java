/* *********************************************************************** *
 * project: org.matsim.*
 * IatbrPlanBuilder.java
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

package playground.southafrica.projects.iatbr2012;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.southafrica.utilities.Header;
import playground.southafrica.utilities.gis.MyMultiFeatureReader;

public class IatbrPlanBuilder {
	private final static Logger LOG = Logger.getLogger(IatbrPlanBuilder.class);
	private static QuadTree<Coord> spot5QT;
	private static int spotId = 0;
	private static Map<Coord, Id<ActivityFacility>> facilityIdMap = new HashMap<Coord, Id<ActivityFacility>>();
	private static ActivityFacilitiesImpl activityFacilities;
	private static QuadTree<ActivityFacilityImpl> sacscQT;
	private static QuadTree<ActivityFacilityImpl> amenityQT;
	private static QuadTree<ActivityFacilityImpl> educationQT;
	private static QuadTree<ActivityFacilityImpl> shoppingQT;
	private static QuadTree<ActivityFacilityImpl> leisureQT;
	private static ObjectAttributes sacscAttributes;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(IatbrPlanBuilder.class.toString(), args);
		
		String configFile = args[0];
		String plansFile = args[1];
		String sacscFile = args[2];
		String sacscAttributeFile = args[3];
		String spot5File = args[4];
		String amenityFile = args[5];
		
		Config config = ConfigUtils.createConfig();
		config.addCoreModules();
		ConfigUtils.loadConfig(config, configFile);		
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		activityFacilities = new ActivityFacilitiesImpl();
		activityFacilities.setName("Facilities for Nelson Mandela Bay Metropolitan");
		
		/* Build QuadTree from Spot 5 facilities. */
		spot5QT = buildSpot5QuadTree(spot5File);
		
		/* READ THE VARIOUS INPUT FILES */	
		/* Read SACSC shopping facilities */
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.parse(sacscFile);
		processSacscQT(sc);
		
		/* Read SACSC shopping facility attributes */
		sacscAttributes = new ObjectAttributes();
		ObjectAttributesXmlReader or = new ObjectAttributesXmlReader(sacscAttributes);
		or.parse(sacscAttributeFile);
		
		/* Read the general amenities file. */
		MutableScenario scAmenities = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimFacilitiesReader mfr = new MatsimFacilitiesReader(scAmenities);
		mfr.parse(amenityFile);
		processAmenities(scAmenities);
		
		/* Read network */
		NetworkReaderMatsimV1 nr = new NetworkReaderMatsimV1(sc.getNetwork());
		nr.parse(sc.getConfig().network().getInputFile());
		LOG.info("Number of links: " + sc.getNetwork().getLinks().size());
		LOG.info("Number of nodes: " + sc.getNetwork().getNodes().size());

		/* Read plans */
		MatsimPopulationReader pr = new MatsimPopulationReader(sc);
		pr.parse(plansFile);
		createPrimaryActivityFacilities(sc.getPopulation());
		
		
		/* Write the facilities to the same file as what the config specifies. 
		 * This is necessary since the controller will later read in this file 
		 * when executing. */
		FacilitiesWriter fw = new FacilitiesWriter(activityFacilities);
		fw.write(sc.getConfig().facilities().getInputFile());
		/* Report on the facility statistics. */
		reportFacilities(activityFacilities);
		
		/* Write the population to the same file as what the config specifies. 
		 * This is necessary since the controller will later read in this file 
		 * when executing. */		
		PopulationWriter pw = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
		pw.writeFileV5(sc.getConfig().plans().getInputFile());
		
		
		/* Try location choice */
//		ConfigUtils.loadConfig(sc.getConfig(), configFile);
//		Controler controler = new Controler(sc);
//		controler.getConfig().locationchoice().setAlgorithm("bestResponse");
//		CharyparNagelScoringFunctionFactory cn = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), sc.getNetwork());
//		controler.setScoringFunctionFactory(cn);
//		controler.setLeastCostPathCalculatorFactory(new FastDijkstraFactory());
//		controler.getConfig().locationchoice().setScaleFactor("100");
//		controler.getConfig().locationchoice().setMaxRecursions("20");
//		controler.getConfig().locationchoice().setFlexibleTypes("s,l,o");
//		controler.getConfig().locationchoice().setEpsilonScaleFactors("0.5,0.5,0.5");
//		LocationChoice lc = new LocationChoice(sc.getNetwork(), controler);
//		lc.prepareReplanning();
//		int count = 0;
//		Iterator<Id> planIterator = sc.getPopulation().getPersons().keySet().iterator();
//		while(count++ < 10 && planIterator.hasNext()){
//			Person person = sc.getPopulation().getPersons().get(planIterator.next());
//			LOG.info("Doing location choice for person " + person.getId());
//			lc.handlePlan(person.getSelectedPlan());
//		}
//		lc.finishReplanning();
		
		/* Run the simulation for one run. */
//		Config configRun = ConfigUtils.createConfig();
//		configRun.addCoreModules();
//		ConfigUtils.loadConfig(configRun, configFile);		
//		controler = new Controler(configRun);
//		controler.setOverwriteFiles(true);
//		controler.run();
		
		Header.printFooter();
	}
	
	private static void reportFacilities(ActivityFacilitiesImpl facilities){
		int s = 0;
		int l = 0;
		int o = 0;
		int h = 0;
		int w = 0;
		int e = 0;
		for(Id<ActivityFacility> id : facilities.getFacilities().keySet()){
			ActivityFacilityImpl  af = (ActivityFacilityImpl) facilities.getFacilities().get(id);
			for(String ao : af.getActivityOptions().keySet()){
				if(ao.equalsIgnoreCase("h")){ h++;
				} else if(ao.startsWith("w")){ w++;
				} else if(ao.startsWith("e")){ e++;
				} else if(ao.startsWith("s")){ s++;
				} else if(ao.startsWith("l")){ l++;
				} else if(ao.startsWith("t")){ o++;
				}
			}
		}
		LOG.info("Number of facilities catering for:");
		LOG.info("   home     : " + h);
		LOG.info("   work     : " + w);
		LOG.info("   education: " + e);
		LOG.info("   shopping : " + s);
		LOG.info("   leisure  : " + l);
		LOG.info("   other    : " + o);
	}
	
	
	/**
	 * Building a {@link QuadTree} from the Spot 5 satellite image facilities.
	 * @param filename
	 * @return
	 */
	private static QuadTree<Coord> buildSpot5QuadTree(String filename){

		/* READ THE SPOT Facilities */
		MyMultiFeatureReader mfr = new MyMultiFeatureReader();
		List<Coord> spot5Coords = mfr.readCoords(filename);
		
		double xMin = Double.POSITIVE_INFINITY;
		double xMax = Double.NEGATIVE_INFINITY;
		double yMin = Double.POSITIVE_INFINITY;
		double yMax = Double.NEGATIVE_INFINITY;
		for(Coord c : spot5Coords){
			xMin = Math.min(xMin, c.getX());
			xMax = Math.max(xMax, c.getX());
			yMin = Math.min(yMin, c.getY());
			yMax = Math.max(yMax, c.getY());
		}
		QuadTree<Coord> qt = new QuadTree<Coord>(xMin, yMin, xMax, yMax);
		for(Coord c : spot5Coords){
			qt.put(c.getX(), c.getY(), c);
		}		
		LOG.info("Number of Spot 5 facilities: " + qt.size());
		return qt;
	}
	
	private static void createPrimaryActivityFacilities(Population population){
		LOG.info("Assigning Spot 5 facilities to activities with unknown facilities...");
		LOG.info("Number of people (before):     " + population.getPersons().size());
		double workAtAmenityThreshold = 20;
		double workAtShoppingThreshold = 500;
		
		List<Id<Person>> agentsToRemove = new ArrayList<Id<Person>>();
		Iterator<Id<Person>> iterator = population.getPersons().keySet().iterator();
		while(iterator.hasNext()){
			Id<Person> id = iterator.next();
			Person person = population.getPersons().get(id);
			ActivityImpl firstActivity = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0));
			ActivityImpl lastActivity = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1));
			if(firstActivity.getType().startsWith("h") && lastActivity.getType().startsWith("h")){

				/* Check for home activity */
				ActivityFacilityImpl home = null;
				Coord closestBuilding = spot5QT.getClosest(firstActivity.getCoord().getX(), firstActivity.getCoord().getY());
				if(!facilityIdMap.containsKey(closestBuilding)){
					Id<ActivityFacility> newFacility = Id.create("spot_" + spotId++, ActivityFacility.class);
					home = activityFacilities.createAndAddFacility(newFacility, closestBuilding);
					home.createAndAddActivityOption("h");
					/* This should not be necessary, but is inconsistent with other containers. */
					/* TODO Follow up with MATSim developers. */
					if(!activityFacilities.getFacilities().containsKey(home.getId())){
						activityFacilities.addActivityFacility(home);
					}
					facilityIdMap.put(closestBuilding, home.getId());
				} else{
					home = (ActivityFacilityImpl) activityFacilities.getFacilities().get(facilityIdMap.get(closestBuilding));
				}
				firstActivity.setFacilityId(home.getId());
				firstActivity.setCoord(home.getCoord());
				lastActivity.setFacilityId(home.getId());
				lastActivity.setCoord(home.getCoord());

				for(int i = 1; i < person.getSelectedPlan().getPlanElements().size()-1; i++){
					PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
					if(pe instanceof Activity){
						ActivityImpl act = (ActivityImpl) pe;
						if(act.getType().startsWith("h")){
							act.setFacilityId(home.getId());
						} else if(act.getType().startsWith("w")){
							/* Check for work activity */
							ActivityFacilityImpl closestMall = sacscQT.getClosest(act.getCoord().getX(), act.getCoord().getY());
							/* First check closest mall, and choose if it is within the threshold. */
							if(closestMall.calcDistance(act.getCoord()) <= workAtShoppingThreshold){
								act.setFacilityId(closestMall.getId());
								act.setCoord(closestMall.getCoord());
							} else{
								/* Second, check closest amenity, and choose if it is within the threshold. */
								ActivityFacilityImpl closestAmenity = amenityQT.getClosest(act.getCoord().getX(), act.getCoord().getY());
								if(closestAmenity.calcDistance(act.getCoord()) <= workAtAmenityThreshold){
									act.setFacilityId(closestAmenity.getId());
									act.setCoord(closestAmenity.getCoord());
								} else{
									/* Just choose the closest Spot 5 building */
									ActivityFacilityImpl work;
									Coord closestBuildingCoord = spot5QT.getClosest(act.getCoord().getX(), act.getCoord().getY());
									if(!facilityIdMap.containsKey(closestBuildingCoord)){
										Id<ActivityFacility> newFacility = Id.create("spot_" + spotId++, ActivityFacility.class);
										ActivityFacilityImpl afi = activityFacilities.createAndAddFacility(newFacility, closestBuildingCoord);
										afi.createAndAddActivityOption("w");
										facilityIdMap.put(closestBuildingCoord, newFacility);
										work = afi;
									} else{
										work = (ActivityFacilityImpl) activityFacilities.getFacilities().get(facilityIdMap.get(closestBuildingCoord));
									}
									act.setFacilityId(work.getId());
									act.setCoord(work.getCoord());
								}
							}
						} else if(act.getType().startsWith("e")){
							/* Education activity. */
							ActivityFacilityImpl closestSchool = educationQT.getClosest(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestSchool.getId());
							act.setCoord(closestSchool.getCoord());
						} else if(act.getType().startsWith("s")){
							/* Pick the closest shopping facility */
							ActivityFacilityImpl closestShop = shoppingQT.getClosest(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestShop.getId());
							act.setCoord(closestShop.getCoord());			
						} else if(act.getType().startsWith("l")){
							/* Pick the closest leisure facility. */
							ActivityFacilityImpl closestLeisure = leisureQT.getClosest(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestLeisure.getId());
							act.setCoord(closestLeisure.getCoord());	
						} else{
							/* Just pick the closest amenity facility. 
							 * I (JWJ Jun '12) realized that the LocationChoice 
							 * algorithm NEEDS all activities to be assigned a
							 * facility.*/
							ActivityFacilityImpl closestAmenity = amenityQT.getClosest(act.getCoord().getX(), act.getCoord().getY());
							act.setFacilityId(closestAmenity.getId());
							act.setCoord(closestAmenity.getCoord());
						}
					}
				}
			} else {
				agentsToRemove.add(person.getId());
			} 
		}
		LOG.info("   Removing " + agentsToRemove.size() + " agents whose plans do not start AND end with `h..'");
		for(Id<Person> id : agentsToRemove){
			population.getPersons().remove(id);
		}
		LOG.info("Number of facilities    : " + activityFacilities.getFacilities().size());
 		LOG.info("Number of people (after): " + population.getPersons().size());
	}
	
	
	private static void processSacscQT(MutableScenario sc){
		sacscQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		leisureQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		shoppingQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		
		for(Id<ActivityFacility> id : sc.getActivityFacilities().getFacilities().keySet()){
			ActivityFacilityImpl af = (ActivityFacilityImpl) sc.getActivityFacilities().getFacilities().get(id);
			ActivityFacilityImpl afNew = activityFacilities.createAndAddFacility(Id.create("sacsc_" + id.toString(), ActivityFacility.class), af.getCoord());
			afNew.getActivityOptions().putAll(af.getActivityOptions());
			sacscQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
			shoppingQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
			leisureQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
			if(!facilityIdMap.containsKey(afNew.getCoord())){
				facilityIdMap.put(afNew.getCoord(), afNew.getId());
			}
		}
		LOG.info("Activity facilities (after SACSC data): " + activityFacilities.getFacilities().size());
	}


	private static void processAmenities(MutableScenario sc){
		amenityQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		educationQT = new QuadTree<ActivityFacilityImpl>(spot5QT.getMinEasting(), spot5QT.getMinNorthing(), spot5QT.getMaxEasting(), spot5QT.getMaxNorthing());
		int droppedCounter = 0;
		double amenityToShoppingCentreThreshold = 100;
		for(Id<ActivityFacility> id: sc.getActivityFacilities().getFacilities().keySet()){
			ActivityFacilityImpl af = (ActivityFacilityImpl) sc.getActivityFacilities().getFacilities().get(id);
			ActivityFacilityImpl closestMall = sacscQT.getClosest(af.getCoord().getX(), af.getCoord().getY());
			if(af.calcDistance(closestMall.getCoord()) > amenityToShoppingCentreThreshold){
				/* Add it as an independent amenity. */
				Coord closestBuildingCoord = spot5QT.getClosest(af.getCoord().getX(), af.getCoord().getY());
				ActivityFacilityImpl afNew;
				if(!facilityIdMap.containsKey(closestBuildingCoord)){
					afNew = activityFacilities.createAndAddFacility(Id.create("osm_" + id.toString(), ActivityFacility.class), closestBuildingCoord);
					afNew.getActivityOptions().putAll(af.getActivityOptions());
					facilityIdMap.put(closestBuildingCoord, afNew.getId());
				} else{
					afNew = (ActivityFacilityImpl) activityFacilities.getFacilities().get(facilityIdMap.get(closestBuildingCoord));
					afNew.getActivityOptions().putAll(af.getActivityOptions());				
				}
				amenityQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				if(afNew.getActivityOptions().containsKey("e")){
					educationQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				}
				if(afNew.getActivityOptions().containsKey("s")){
					shoppingQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				}
				if(afNew.getActivityOptions().containsKey("l")){
					leisureQT.put(afNew.getCoord().getX(), afNew.getCoord().getY(), afNew);
				}
			} else{
				droppedCounter++;
			}
		}
		LOG.info("Number of OSM amenities dropped in favour of SACSC malls: " + droppedCounter);
		LOG.info("Activity facilities (after OSM amenities): " + activityFacilities.getFacilities().size());
	}

}

