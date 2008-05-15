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

package playground.meisterk.facilities;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesProductionKTI;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.facilities.algorithms.FacilityAlgorithm;
import org.matsim.gbl.Gbl;
import org.matsim.utils.misc.Day;
import org.matsim.utils.misc.Time;
import org.matsim.world.Location;

/**
 * Assign every shop an opening time based on shopsOf2005 survey.
 * 
 * @author meisterk
 *
 */
public class FacilitiesOpentimesKTIYear2 extends FacilityAlgorithm {

	private Facilities shopsOf2005 = new Facilities("shopsOf2005", Facilities.FACILITIES_NO_STREAMING);

	private String shopsOf2005Filename = "/home/meisterk/sandbox00/ivt/studies/switzerland/facilities/shopsOf2005/facilities_shopsOf2005.xml";

	public FacilitiesOpentimesKTIYear2() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void init() {

		System.out.println("Reading shops Of 2005 xml file... ");
		FacilitiesReaderMatsimV1 facilities_reader = new FacilitiesReaderMatsimV1(shopsOf2005);
		facilities_reader.readFile(shopsOf2005Filename);
		System.out.println("Reading shops Of 2005 xml file...done.");

	}

	@Override
	public void run(Facility facility) {

		Day[] days = Day.values();
		double startTime = -1.0;
		double endTime = -1.0;

		TreeMap<String, TreeSet<Opentime>> closestShopOpentimes = new TreeMap<String, TreeSet<Opentime>>();

		ArrayList<Location> closestShops = shopsOf2005.getNearestLocations(facility.getCenter());
		Activity shopsOf2005ShopAct = ((Facility) closestShops.get(0)).getActivity(FacilitiesProductionKTI.ACT_TYPE_SHOP);
		if (shopsOf2005ShopAct != null) {
			closestShopOpentimes = shopsOf2005ShopAct.getOpentimes();
		}
		TreeMap<String, Activity> activities = facility.getActivities();

		// if only presence code and work are present
		switch(activities.size()){
		case 2:
			// standard daily opentimes for industry sector
			if (activities.containsKey(FacilitiesProductionKTI.WORK_SECTOR2)) {
				for (Day day : days) {
					if (
							day.equals(Day.MONDAY) ||
							day.equals(Day.TUESDAY) ||
							day.equals(Day.WEDNESDAY) ||
							day.equals(Day.THURSDAY) ||
							day.equals(Day.FRIDAY)) {
						activities.get(FacilitiesProductionKTI.WORK_SECTOR2).createOpentime(
								day.getAbbrevEnglish(), 
								Time.writeTime(7.0 * 3600), 
								Time.writeTime(18.0 * 3600));
					}
				}
				// open times of the closest shop for services sector
			} else if (activities.containsKey(FacilitiesProductionKTI.WORK_SECTOR3)) {
				// eliminate lunch break
				for (Day day : days) {
					TreeSet<Opentime> dailyOpentime = closestShopOpentimes.get(day.getAbbrevEnglish());
					if (dailyOpentime != null) {
						switch(dailyOpentime.size()) {
						case 2:
							startTime = Math.min(
									((Opentime) dailyOpentime.toArray()[0]).getStartTime(), 
									((Opentime) dailyOpentime.toArray()[1]).getStartTime());
							endTime = Math.max(
									((Opentime) dailyOpentime.toArray()[0]).getEndTime(), 
									((Opentime) dailyOpentime.toArray()[1]).getEndTime());
							break;
						case 1:
							startTime = ((Opentime) dailyOpentime.toArray()[0]).getStartTime();
							endTime = ((Opentime) dailyOpentime.toArray()[0]).getEndTime();
							break;
						}
						activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
								day.getAbbrevEnglish(),
								Time.writeTime(startTime), 
								Time.writeTime(endTime));
					}
				}
			}
		// if presence code, work and one other imputed activity are present
		case 3:
			for (String activityType : activities.keySet()) {
				if (
						Pattern.matches(FacilitiesProductionKTI.ACT_TYPE_SHOP + ".*", activityType)) {
					activities.get(activityType).setOpentimes(closestShopOpentimes);
					activities.get(FacilitiesProductionKTI.WORK_SECTOR3).setOpentimes(closestShopOpentimes);
				} else if (
						Pattern.matches(FacilitiesProductionKTI.EDUCATION_KINDERGARTEN, activityType) ||
						Pattern.matches(FacilitiesProductionKTI.EDUCATION_PRIMARY, activityType)) {
					for (Day day : days) {
						if (
								day.equals(Day.MONDAY) ||
								day.equals(Day.TUESDAY) ||
								day.equals(Day.WEDNESDAY) ||
								day.equals(Day.THURSDAY) ||
								day.equals(Day.FRIDAY)) {
							activities.get(activityType).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(8.0 * 3600), 
									Time.writeTime(12.0 * 3600));
							activities.get(activityType).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(13.5 * 3600), 
									Time.writeTime(17.0 * 3600));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(8.0 * 3600), 
									Time.writeTime(17.0 * 3600));
						}
					}
				} else if (
						Pattern.matches(FacilitiesProductionKTI.EDUCATION_SECONDARY, activityType) ||
						Pattern.matches(FacilitiesProductionKTI.EDUCATION_OTHER, activityType)) {
					for (Day day : days) {
						if (
								day.equals(Day.MONDAY) ||
								day.equals(Day.TUESDAY) ||
								day.equals(Day.WEDNESDAY) ||
								day.equals(Day.THURSDAY) ||
								day.equals(Day.FRIDAY)) {
							activities.get(activityType).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(8.0 * 3600), 
									Time.writeTime(18.0 * 3600));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(8.0 * 3600), 
									Time.writeTime(18.0 * 3600));
						}
					}
				} else if (
						Pattern.matches(FacilitiesProductionKTI.EDUCATION_HIGHER, activityType)) {
					for (Day day : days) {
						if (
								day.equals(Day.MONDAY) ||
								day.equals(Day.TUESDAY) ||
								day.equals(Day.WEDNESDAY) ||
								day.equals(Day.THURSDAY) ||
								day.equals(Day.FRIDAY)) {
							activities.get(activityType).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(7.0 * 3600), 
									Time.writeTime(22.0 * 3600));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(7.0 * 3600), 
									Time.writeTime(22.0 * 3600));
						} else if (day.equals(Day.SATURDAY)) {
							activities.get(activityType).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(8.0 * 3600), 
									Time.writeTime(12.0 * 3600));
							activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
									day.getAbbrevEnglish(), 
									Time.writeTime(8.0 * 3600), 
									Time.writeTime(12.0 * 3600));
						}
					}
				} else if (
						Pattern.matches(FacilitiesProductionKTI.LEISURE_SPORTS, activityType)) {
					for (Day day : days) {
						if (
								day.equals(Day.MONDAY) ||
								day.equals(Day.TUESDAY) ||
								day.equals(Day.WEDNESDAY) ||
								day.equals(Day.THURSDAY) ||
								day.equals(Day.FRIDAY)) {
							startTime = 9.0 * 3600;
							endTime = 22.0 * 3600;
						} else if (
								day.equals(Day.SATURDAY) ||
								day.equals(Day.SUNDAY)) {
							startTime = 9.0 * 3600;
							endTime = 18.0 * 3600;
						}
						activities.get(activityType).createOpentime(
								day.getAbbrevEnglish(), 
								Time.writeTime(startTime), 
								Time.writeTime(endTime));
						activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
								day.getAbbrevEnglish(), 
								Time.writeTime(startTime), 
								Time.writeTime(endTime));
					}
				} else if (
						Pattern.matches(FacilitiesProductionKTI.LEISURE_GASTRO, activityType)) {
					for (Day day : days) {
						startTime = 9.0 * 3600;
						endTime = 24.0 * 3600;
						activities.get(activityType).createOpentime(
								day.getAbbrevEnglish(), 
								Time.writeTime(startTime), 
								Time.writeTime(endTime));
						activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
								day.getAbbrevEnglish(), 
								Time.writeTime(startTime), 
								Time.writeTime(endTime));
					}					
				} else if (
						Pattern.matches(FacilitiesProductionKTI.LEISURE_CULTURE, activityType)) {
					for (Day day : days) {
						startTime = 14.0 * 3600;
						endTime = 24.0 * 3600;
						activities.get(activityType).createOpentime(
								day.getAbbrevEnglish(), 
								Time.writeTime(startTime), 
								Time.writeTime(endTime));
						activities.get(FacilitiesProductionKTI.WORK_SECTOR3).createOpentime(
								day.getAbbrevEnglish(), 
								Time.writeTime(startTime), 
								Time.writeTime(endTime));
					}					
				}
			}
		}
	}
	
}
