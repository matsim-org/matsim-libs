/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesOpentimesKTIYear2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.facilities.algorithms;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.facilities.algorithms.AbstractFacilityAlgorithm;
import playground.meisterk.org.matsim.run.facilities.FacilitiesProductionKTI;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Pattern;

/**
 * Assign every shop an opening time based on shopsOf2005 survey.
 *
 * @author meisterk
 *
 */
public class FacilitiesOpentimesKTIYear2 extends AbstractFacilityAlgorithm {

	private final Scenario scenario;
	private final ActivityFacilities shopsOf2005;

	private final String shopsOf2005Filename = "/home/meisterk/sandbox00/ivt/studies/switzerland/facilities/shopsOf2005/facilities_shopsOf2005.xml";

	private static final Logger log = Logger.getLogger(FacilitiesOpentimesKTIYear2.class);

	public FacilitiesOpentimesKTIYear2() {
		super();
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.shopsOf2005 = scenario.getActivityFacilities();
		this.shopsOf2005.setName("shopsOf2005");
	}

	public void init() {

		System.out.println("Reading shops Of 2005 xml file... ");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(this.scenario);
		facilities_reader.readFile(this.shopsOf2005Filename);
		System.out.println("Reading shops Of 2005 xml file...done.");

	}

	@Override
	public void run(final ActivityFacility facility) {

		double startTime = -1.0;
		double endTime = -1.0;

		SortedSet<OpeningTime> closestShopOpentimes = null;

//		 List<? extends BasicLocation> closestShops = this.shopsOf2005.getNearestLocations(facility.getCoord());
		List<? extends BasicLocation> closestShops = null; // destroyed by refactoring
		ActivityOptionImpl shopsOf2005ShopAct = (ActivityOptionImpl) ((ActivityFacilityImpl) closestShops.get(0)).getActivityOptions().get(FacilitiesProductionKTI.ACT_TYPE_SHOP);
		if (shopsOf2005ShopAct != null) {
			closestShopOpentimes = shopsOf2005ShopAct.getOpeningTimes();
		} else {
			log.info("shop activity object of closest shop facility is null.");
		}
		Map<String, ? extends ActivityOption> activities = facility.getActivityOptions();

		// remove all existing opentimes
		for (ActivityOption a : activities.values()) {
			((ActivityOptionImpl) a).clearOpeningTimes();
		}

		// if only presence code and work are present
		switch(activities.size()){
			case 2:
				// standard daily opentimes for industry sector
				if (activities.containsKey(FacilitiesProductionKTI.WORK_SECTOR2)) {
						activities.get(FacilitiesProductionKTI.WORK_SECTOR2).addOpeningTime(new OpeningTimeImpl(
								7.0 * 3600,
								18.0 * 3600));
					// open times of the closest shop for services sector
				} else if (activities.containsKey(FacilitiesProductionKTI.WORK_SECTOR3)) {
					// eliminate lunch break
						SortedSet<OpeningTime> dailyOpentime = closestShopOpentimes;
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
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									startTime,
									endTime));
					}
				}
				break;
				// if presence code, work and one other imputed activity are present
			case 3:
				for (String activityType : activities.keySet()) {
					if (
							Pattern.matches(FacilitiesProductionKTI.ACT_TYPE_SHOP + ".*", activityType)) {
						((ActivityOptionImpl) activities.get(activityType)).setOpeningTimes(closestShopOpentimes);
						((ActivityOptionImpl) activities.get(FacilitiesProductionKTI.WORK_SECTOR3)).setOpeningTimes(closestShopOpentimes);
					} else if (
							Pattern.matches(FacilitiesProductionKTI.EDUCATION_KINDERGARTEN, activityType) ||
							Pattern.matches(FacilitiesProductionKTI.EDUCATION_PRIMARY, activityType)) {
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									8.0 * 3600,
									12.0 * 3600));
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									13.5 * 3600,
									17.0 * 3600));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									8.0 * 3600,
									17.0 * 3600));
					} else if (
							Pattern.matches(FacilitiesProductionKTI.EDUCATION_SECONDARY, activityType) ||
							Pattern.matches(FacilitiesProductionKTI.EDUCATION_OTHER, activityType)) {
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									8.0 * 3600,
									18.0 * 3600));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									8.0 * 3600,
									18.0 * 3600));
					} else if (
							Pattern.matches(FacilitiesProductionKTI.EDUCATION_HIGHER, activityType)) {
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									7.0 * 3600,
									22.0 * 3600));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									7.0 * 3600,
									22.0 * 3600));
					} else if (
							Pattern.matches(FacilitiesProductionKTI.LEISURE_SPORTS, activityType)) {
								startTime = 9.0 * 3600;
								endTime = 22.0 * 3600;
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									startTime,
									endTime));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									startTime,
									endTime));
					} else if (
							Pattern.matches(FacilitiesProductionKTI.LEISURE_GASTRO, activityType)) {
							startTime = 9.0 * 3600;
							endTime = 24.0 * 3600;
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									startTime,
									endTime));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									startTime,
									endTime));
					} else if (
							Pattern.matches(FacilitiesProductionKTI.LEISURE_CULTURE, activityType)) {
							startTime = 14.0 * 3600;
							endTime = 24.0 * 3600;
							activities.get(activityType).addOpeningTime(new OpeningTimeImpl(
									startTime,
									endTime));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).addOpeningTime(new OpeningTimeImpl(
									startTime,
									endTime));
					}
				}
		}
	}

}
