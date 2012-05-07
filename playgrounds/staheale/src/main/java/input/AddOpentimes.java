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

package input;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Coord;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.algorithms.AbstractFacilityAlgorithm;


/**
 * Assign every shop an opening time based on shopsOf2005 survey.
 * 
 * -->adapted to new classification by staha
 * 
 * @author meisterk
 *
 */
public class AddOpentimes extends AbstractFacilityAlgorithm {

	private final ScenarioImpl scenario;
	private final ActivityFacilitiesImpl shopsOf2005;
	private final String shopsOf2005Filename = "input/facilities_shopsOf2005.xml";
	private static final Logger log = Logger.getLogger(AddOpentimes.class);
	//private TreeMap<Id, shopsOf2005> shops = new TreeMap<Id, shopsOf2005>();
	private QuadTree<ActivityOptionImpl> shoppingQuadTree;


	public AddOpentimes(final ScenarioImpl scenario) {
		super();
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.shopsOf2005 = scenario.getActivityFacilities();
		this.shopsOf2005.setName("shopsOf2005");
		this.shoppingQuadTree = this.buildShopsQuadTree(shopsOf2005);
		log.info(" shoppingQuadTree size: " +this.shoppingQuadTree.size());
	}

	public void init() {
		log.info("Reading shops Of 2005 xml file... ");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(this.scenario);
		facilities_reader.readFile(this.shopsOf2005Filename);
		log.info("Reading shops Of 2005 xml file...done.");
	}
	
	private QuadTree<ActivityOptionImpl> buildShopsQuadTree(ActivityFacilitiesImpl shopsOf2005) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		TreeMap<Id,ActivityFacility> shoppingFacilities = this.shopsOf2005.getFacilitiesForActivityType("shop");
		
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

		QuadTree<ActivityOptionImpl> quadtree = new QuadTree<ActivityOptionImpl>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : shoppingFacilities.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityOptionImpl) f);
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
		
		Map<DayType, SortedSet<OpeningTime>> closestShopOpentimes = new TreeMap<DayType, SortedSet<OpeningTime>>();

	//	log.info("TreeMap defined");
		
		Map<String, ? extends ActivityOption> activities = facility.getActivityOptions();

		
		if (activities.containsKey(FacilitiesProduction.ACT_TYPE_SHOP_RETAIL)) {
			Double x = facility.getCoord().getX();
			Double y = facility.getCoord().getY();
			ActivityOptionImpl closestShop = this.shoppingQuadTree.get(x,y);
			closestShopOpentimes = closestShop.getOpeningTimes();	
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
			((ActivityOptionImpl) a).setOpeningTimes(new TreeMap<DayType, SortedSet<OpeningTime>>());
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
						for (DayType day : days) {
							((ActivityOptionImpl) activities.get(activityType)).setOpeningTimes(closestShopOpentimes);
							SortedSet<OpeningTime> dailyOpentime = closestShopOpentimes.get(day);
							if (dailyOpentime != null) {
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
										day,
										startTime,
										endTime));
							}
						}
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
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
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
									day.equals(DayType.sat)) {
								startTime = 0.0 * 3600;
								endTime = 3.0 * 3600;								
								activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
								DayType.sat,
								16.0 * 3600,
								24.0 * 3600));
							} else if (
									day.equals(DayType.sun)) {
								startTime = 0.0 * 3600;
								endTime = 04.0 * 3600;								
								activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
								DayType.sun,
								16.0 * 3600,
								24.0 * 3600));
							}
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
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
							activityType == "B015530A" || activityType == "B015551A"|| activityType == "B019253A"
							) {
						for (DayType day : days) {
							startTime = 9.0 * 3600;
							endTime = 24.0 * 3600;
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
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
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
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
							
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
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
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
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