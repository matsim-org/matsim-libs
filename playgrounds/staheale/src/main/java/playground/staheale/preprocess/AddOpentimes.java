/* *********************************************************************** *
 * project: org.matsim.*
 * AddOpentimes.java
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

//import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
//import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.facilities.OpeningTime.DayType;
import org.matsim.facilities.algorithms.AbstractFacilityAlgorithm;
//import org.matsim.core.gbl.Gbl;


/**
 * Assign every shop an opening time based on shopsOf2005 survey.
 * 
 * -->adapted to new classification by staha
 * 
 * @author meisterk
 *
 */
public class AddOpentimes extends AbstractFacilityAlgorithm {

	private Scenario scenario;
	private ActivityFacilities shopsOf2005;
	private final String shopsOf2005Filename = "input/facilities_shopsOf2005.xml";
	private static final Logger log = Logger.getLogger(AddOpentimes.class);
	//private TreeMap<Id, shopsOf2005> shops = new TreeMap<Id, shopsOf2005>();
	private QuadTree<ActivityFacility> shoppingQuadTree;


	public AddOpentimes() {
		super();
		
	}

	public void init() {
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		log.info("Reading shops Of 2005 xml file... ");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(this.scenario);
		facilities_reader.readFile(this.shopsOf2005Filename);
		log.info("Reading shops Of 2005 xml file...done.");
		this.shopsOf2005 = scenario.getActivityFacilities();
		this.shopsOf2005.setName("shopsOf2005");
		log.info("shopsOf2005 size: " +shopsOf2005.getFacilities().size());
		TreeMap<Id<ActivityFacility>, ActivityFacility> shoppingFacilities = this.shopsOf2005.getFacilitiesForActivityType("shop");
		this.shoppingQuadTree = this.buildShopsQuadTree(shoppingFacilities);
		log.info(" shoppingQuadTree size: " +this.shoppingQuadTree.size());
		
	}
	
	private QuadTree<ActivityFacility> buildShopsQuadTree(TreeMap<Id<ActivityFacility>, ActivityFacility> shoppingFacilities) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (final ActivityFacility f : shoppingFacilities.values()) {
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
		for (final ActivityFacility f : shoppingFacilities.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacility) f);
		}
		return quadtree;
	}
	
	@Override
	public void run(final ActivityFacility facility) {
		//log.info("starting Opentimes run");


		DayType[] days = new DayType[] { DayType.mon, DayType.tue, DayType.wed, DayType.thu, DayType.fri, DayType.sat, DayType.sun };
		DayType[] weekDays = new DayType[] { DayType.mon, DayType.tue, DayType.wed, DayType.thu, DayType.fri };
		DayType[] museumDays = new DayType[] { DayType.tue, DayType.wed, DayType.thu, DayType.fri, DayType.sat, DayType.sun };
		DayType[] libraryDays = new DayType[] { DayType.mon, DayType.tue, DayType.wed, DayType.thu, DayType.fri, DayType.sat };

		double startTime = -1.0;
		double endTime = -1.0;

		//log.info("day types defined");

		//closest shop
		
		SortedSet<OpeningTime> closestShopOpentimes = null;

	//	log.info("TreeMap defined");
		
		Map<String, ? extends ActivityOption> activities = facility.getActivityOptions();

		//for (String key: activities.keySet()){
			//log.info(key);
		//}
		
		if (activities.containsKey(FacilitiesProduction.SHOP_RETAIL_OTHER) || activities.containsKey(FacilitiesProduction.SHOP_RETAIL_GT2500) || activities.containsKey(FacilitiesProduction.SHOP_RETAIL_GET1000) || activities.containsKey(FacilitiesProduction.SHOP_RETAIL_GET400) || activities.containsKey(FacilitiesProduction.SHOP_RETAIL_GET100) || activities.containsKey(FacilitiesProduction.SHOP_RETAIL_LT100)) {
			Double x = facility.getCoord().getX();
			Double y = facility.getCoord().getY();
			ActivityFacility closestShop = this.shoppingQuadTree.get(x,y);
			//log.info("closestShop size: "+ closestShop.getActivityOptions().size());
			if (closestShop.getActivityOptions().get("shop").getOpeningTimes()!=null) {closestShopOpentimes = closestShop.getActivityOptions().get("shop").getOpeningTimes();}
//			if (closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.mon)!=null) {closestShopOpentimes.put(DayType.mon, closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.mon));}
//			if (closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.tue)!=null) {closestShopOpentimes.put(DayType.tue, closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.tue));}
//			if (closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.wed)!=null) {closestShopOpentimes.put(DayType.wed, closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.wed));}
//			if (closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.thu)!=null) {closestShopOpentimes.put(DayType.thu,closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.thu));}
//			if (closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.fri)!=null) {closestShopOpentimes.put(DayType.fri, closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.fri));}
//			if (closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.sat)!=null) {closestShopOpentimes.put(DayType.sat,closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.sat));}
//			if (closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.sun)!=null) {closestShopOpentimes.put(DayType.sun,closestShop.getActivityOptions().get("shop").getOpeningTimes(DayType.sun));}
		}
		
		//List<MappedLocation> closestShops = this.shopsOf2005.getNearestLocations(facility.getCoord());
		//List<? extends BasicLocation> closestShops = null; // destroyed by refactoring
		//ActivityOptionImpl shopsOf2005ShopAct = (ActivityOptionImpl) ((ActivityFacilityImpl) closestShops.get(0)).getActivityOptions().get(FacilitiesProduction.ACT_TYPE_SHOP_RETAIL);
		//if (shopsOf2005ShopAct != null) {
		//	closestShopOpentimes = shopsOf2005ShopAct.getOpeningTimes();
		//} else {
		//	log.info("shop activity object of closest shop facility is null.");
		//}
		

		//log.info("start removing existing opentimes");

		// remove all existing opentimes
		for (ActivityOption a : activities.values()) {
			((ActivityOptionImpl) a).clearOpeningTimes();
		}
	//	log.info("existing opentimes removed");


		// if only presence code and work are present
		switch(activities.size()){
			case 2:
				// standard daily opentimes for industry sector
				if (activities.containsKey(FacilitiesProduction.WORK_SECTOR2)) {
					for (DayType day : weekDays) {
						activities.get(FacilitiesProduction.WORK_SECTOR2).addOpeningTime(new OpeningTimeImpl(
								day,
								7.0 * 3600,
								18.0 * 3600));
					}
		//			log.info("opentimes for work sector2 defined");

					// open times of the closest shop for services sector
				} else if (activities.containsKey(FacilitiesProduction.WORK_SECTOR3)) {
		//			log.info("start defining opentimes for work sector 3");
		//			log.info("start eliminating lunch break");
					for (DayType day : weekDays) {
						activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
								day,
								8.0 * 3600,
								18.0 * 3600));
					}
			//		log.info("opentimes for work sector3 defined");
				}
					
				break;
				
				// if presence code, work and one other imputed activity are present
			case 3:
				for (String activityType : activities.keySet()) {
					if (
							Pattern.matches(FacilitiesProduction.ACT_TYPE_SHOP_RETAIL + ".*", activityType)) {
						// eliminate lunch break
							for (OpeningTime ot : closestShopOpentimes) {
								((ActivityOptionImpl) activities.get(activityType)).addOpeningTime(ot);
							}
							SortedSet<OpeningTime> dailyOpentime = closestShopOpentimes;
								switch(dailyOpentime.size()) {
									case 2:
										startTime = Math.min(
												((OpeningTimeImpl) dailyOpentime.toArray()[0]).getStartTime(),
												((OpeningTimeImpl) dailyOpentime.toArray()[1]).getStartTime());
										endTime = Math.max(
												((OpeningTimeImpl) dailyOpentime.toArray()[0]).getEndTime(),
												((OpeningTimeImpl) dailyOpentime.toArray()[1]).getEndTime());
										break;
									case 1:
										startTime = ((OpeningTimeImpl) dailyOpentime.toArray()[0]).getStartTime();
										endTime = ((OpeningTimeImpl) dailyOpentime.toArray()[0]).getEndTime();
										break;
								}
								activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
										startTime,
										endTime));
						//((ActivityOptionImpl) activities.get(FacilitiesProduction.WORK_SECTOR3)).setOpeningTimes(closestShopOpentimes);
			//			log.info("opentimes for shop retail defined");
					} else if (
							Pattern.matches(FacilitiesProduction.ACT_TYPE_SHOP_SERVICE, activityType)) {
						for (DayType day : weekDays) {
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									day,
									8.0 * 3600,
									18.0 * 3600));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									8.0 * 3600,
									18.0 * 3600));
			//				log.info("opentimes for shop service defined");
						}
					} else if (
							Pattern.matches(FacilitiesProduction.EDUCATION_KINDERGARTEN, activityType) ||
							Pattern.matches(FacilitiesProduction.EDUCATION_PRIMARY, activityType)) {
						for (DayType day : weekDays) {
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									day,
									8.0 * 3600,
									12.0 * 3600));
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									day,
									13.5 * 3600,
									17.0 * 3600));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									8.0 * 3600,
									17.0 * 3600));
						}
					} else if (
							Pattern.matches(FacilitiesProduction.EDUCATION_SECONDARY, activityType) ||
							Pattern.matches(FacilitiesProduction.EDUCATION_OTHER, activityType)) {
						for (DayType day : weekDays) {
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									day,
									8.0 * 3600,
									18.0 * 3600));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									8.0 * 3600,
									18.0 * 3600));
						}
					} else if (
							Pattern.matches(FacilitiesProduction.EDUCATION_HIGHER, activityType)) {
						for (DayType day : weekDays) {
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									day,
									7.0 * 3600,
									22.0 * 3600));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									7.0 * 3600,
									22.0 * 3600));
						}
						activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
								DayType.sat,
								8.0 * 3600,
								12.0 * 3600));
						activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
								DayType.sat,
								8.0 * 3600,
								12.0 * 3600));

					} 
					//sport
					else if (
							activityType == "B019234A" || activityType == "B019261A"|| activityType == "B019262A"|| activityType == "B019262B" || activityType == "B019272A" || activityType == "B019304A"|| activityType == "B019304B"|| activityType == "B019304C"
							) {
						for (DayType day : days) {
							if (
									day.equals(DayType.mon) ||
									day.equals(DayType.tue) ||
									day.equals(DayType.wed) ||
									day.equals(DayType.thu) ||
									day.equals(DayType.fri)) {
								startTime = 9.0 * 3600;
								endTime = 22.0 * 3600;
							} else if (
									day.equals(DayType.sat) ||
									day.equals(DayType.sun)) {
								startTime = 9.0 * 3600;
								endTime = 20.0 * 3600;
							}
							activities.get(FacilitiesProduction.SPORTS_FUN).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					} 
					//bar, disco, casino
					else if (
							activityType == "B015540A" || activityType == "B019234B" || activityType == "B019234C"|| activityType == "B019271A"
							) {
						for (DayType day : days) {
							if (
									day.equals(DayType.mon) ||
									day.equals(DayType.tue) ||
									day.equals(DayType.wed) ||
									day.equals(DayType.thu) ||
									day.equals(DayType.fri)) {
								startTime = 9.0 * 3600;
								endTime = 24.0 * 3600;
							} else if (
									day.equals(DayType.sat)||
									day.equals(DayType.sun)) {
								startTime = 16.0 * 3600;
								endTime = 24.0 * 3600;;
								activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
								DayType.sun,
								16.0 * 3600,
								24.0 * 3600));
							}
							activities.get(FacilitiesProduction.SPORTS_FUN).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					} 
					//restaurant (+natural parks...)
					else if (
							activityType == "B015530A" || activityType == "B015551A"
							) {
						for (DayType day : days) {
							startTime = 9.0 * 3600;
							endTime = 24.0 * 3600;
							activities.get(FacilitiesProduction.GASTRO_CULTURE).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					}
					//theater, cinema, orchestra
					else if (
							activityType == "B019234D" || activityType == "B019213A" || activityType == "B019231A" || activityType == "B019231B"
							) {
						for (DayType day : days) {
							startTime = 14.0 * 3600;
							endTime = 24.0 * 3600;
							activities.get(FacilitiesProduction.GASTRO_CULTURE).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					}
					//libraries, file rooms
					else if (
							activityType == "B019251A"
							) {
						for (DayType day : libraryDays) {
							if (
									day.equals(DayType.mon) ||
									day.equals(DayType.tue) ||
									day.equals(DayType.wed) ||
									day.equals(DayType.thu) ||
									day.equals(DayType.fri)) {
								startTime = 8.0 * 3600;
								endTime = 18.0 * 3600;
							} else if (
									day.equals(DayType.sat)) {
								startTime = 9.0 * 3600;
								endTime = 16.0 * 3600;
							}
							
							activities.get(FacilitiesProduction.GASTRO_CULTURE).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					} 
					//zoo
					else if (
							activityType == "B019253A"
							) {
						for (DayType day : days) {
							startTime = 09.0 * 3600;
							endTime = 18.0 * 3600;
							activities.get(FacilitiesProduction.GASTRO_CULTURE).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					}
					//amusement park
					else if (
							activityType == "B019233A"
							) {
						for (DayType day : days) {
							startTime = 09.0 * 3600;
							endTime = 18.0 * 3600;
							activities.get(FacilitiesProduction.SPORTS_FUN).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					}
					//museum
					else if (
							activityType == "B019252A"
							) {
						for (DayType day : museumDays) {
							if (
									day.equals(DayType.tue) ||
									day.equals(DayType.wed) ||
									day.equals(DayType.thu) ||
									day.equals(DayType.fri) ||
									day.equals(DayType.sat)) {
								startTime = 10.0 * 3600;
								endTime = 18.0 * 3600;
							} else if (
									day.equals(DayType.sun)) {
								startTime = 10.0 * 3600;
								endTime = 16.0 * 3600;
							}
							activities.get(FacilitiesProduction.GASTRO_CULTURE).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
							activities.get(FacilitiesProduction.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									day,
									startTime,
									endTime));
						}
					} 
				}
		//		log.info("open times added for third case");
		}
	}

}