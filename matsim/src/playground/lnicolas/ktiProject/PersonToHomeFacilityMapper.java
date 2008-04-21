/* *********************************************************************** *
 * project: org.matsim.*
 * PersonToHomeFacilityMapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.ktiProject;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.geometry.CoordI;
import org.matsim.world.Zone;

/**
 * Assumes that each person has a plan with at least one activity of type <i>"h"</i> that
 * contains a coordinate {@code C}. All the coordinates of the
 * person's activities of type <i>"h"</i> are then adjusted to the coordinate of the nearest
 * home facility to {@code C} within the zone (municipality) the person lives in.
 * @author lnicolas
 */
public class PersonToHomeFacilityMapper  {

	protected ArrayList<Person> population;
	protected ArrayList<Zone> zones;
	protected GroupFacilitiesPerZone facilities;

	public final static String homeActType = "home";

	/**
	 * @param population A person at index {@code i} in {@code population} must live (have its home location) within
	 * the zone in {@code zones} at index {@code i}
	 * @param zones A person at index {@code i} in {@code population} must live (have its home location) within
	 * the zone in {@code zones} at index {@code i}
	 * @param facilities The facilities grouped by zones.
	 */
	PersonToHomeFacilityMapper(ArrayList<Person> population,
			ArrayList<Zone> zones, GroupFacilitiesPerZone facilities) {
		this.population = population;
		this.zones = zones;
		this.facilities = facilities;
	}

//	private ArrayList<Facility> getHomeFacilities(Facilities facilities) {
//		ArrayList<Facility> homeFacilities = new ArrayList<Facility>();
//		for (Facility facility : facilities.getFacilities().values()) {
//			// check if the facility contains an activity type which is the same as this.act_type
//			Iterator<String> at_it = facility.getActivities().keySet().iterator();
//			while (at_it.hasNext()) {
//				String at = at_it.next();
//				if (at.equals(homeActType)) {
//					homeFacilities.add(facility);
//					break;
//				}
//			}
//		}
//
//		return homeFacilities;
//	}

	public void run() {
		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		for (int i = 0; i < population.size(); i++) {
			Zone z = run(population.get(i), zones.get(i));
			// Adjust the zone if there is no home facility in the given zone
			zones.set(i, z);

			if (i % (population.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}

		System.out.println();
	}

	public Zone run(Person person, Zone zone) {
		CoordI homeCoord = getHomeCoord(person);
		if (homeCoord != null) {
			Facility facility = facilities.getNearestHomeFacility(
					zone.getId(), homeCoord);
			if (facility == null) {
				zone = facilities.getNearestZone(zone.getId(),
						PlansGenerator.homeActType);
				facility = facilities.getNearestHomeFacility(
						zone.getId(), homeCoord);
			}
//			getNearestFacility(homeCoord, zone, homeActType);
			if (facility != null) {
				setActCoord(person, facility.getCenter(),
						PlansGenerator.homeActType);
//				addHomeActivityFacility(person, facility);
			} else {
				Gbl.errorMsg("No home facility near coord "
						+ homeCoord + " in zone " + zone.getName()
						+ " (" + zone.getId() + ") found!");
			}
		}

		return zone;
	}

//	/**
//	 * Returns the nearest facility (distributed by the number of persons in each facility, i.e.
//	 * it is more possible that a facility with a lot of persons is returned)
//	 * within the given Location
//	 * @param location
//	 * @return a random facility
//	 */
//	private Facility getNearestFacility(Coord coord, Location location,
//			String activityType) {
//		double nearestDist = Double.POSITIVE_INFINITY;
//		Facility nearestFacility = null;
//		double dist = 0;
//		Set<IdI> facilityIds = location.getDownMapping().keySet();
//		for (IdI facilityId : facilityIds) {
//			Facility facility = facilities.getFacility(facilityId);
//			Activity activity = facility.getActivity(activityType);
//			if (activity != null && activity.getCapacity() > 0) {
//				dist = facility.getCoord().calcDistance(coord);
//				if (dist < nearestDist) {
//					nearestDist = dist;
//					nearestFacility = facility;
//				}
//			}
//		}
//
//		return nearestFacility;
//	}

//	private void addHomeActivityFacility(Person person, Facility facility) {
//		Knowledge knowledge = person.createKnowledge(
//				"Created based on enterprise census of 2000");
//		ActivityFacilities actFac = knowledge.createActivityFacility(homeActType);
//		actFac.addFacility(facility);
//	}

	private static void setActCoord(Plan plan, CoordI coord, String actType) {
		BasicPlanImpl.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			Act act = (Act) it.next();
			if (act.getType().equals(actType)) {
				act.setCoord(coord);
			}
		}
	}

	public static void setActCoord(Person person, CoordI coord,
			String actType) {
		for (Plan plan : person.getPlans()) {
			setActCoord(plan, coord, actType);
		}
	}

//	private Facility getNearestHomeFacility(Coord coord) {
//		double nearestDist = Double.POSITIVE_INFINITY;
//		Facility nearestFacility = null;
//		double dist = 0;
//		for (Facility facility : homeFacilities) {
//			dist = facility.getCoord().calcDistance(coord);
//			if (dist < nearestDist) {
//				nearestDist = dist;
//				nearestFacility = facility;
//			}
//		}
//
//		return nearestFacility;
//	}

	private static CoordI getHomeCoord(Plan plan) {
		BasicPlanImpl.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			Act act = (Act) it.next();
			if (act.getType().equals(PlansGenerator.homeActType)) {
				return act.getCoord();
			}
		}

		return null;
	}

	public static CoordI getHomeCoord(Person person) {
		for (Plan plan : person.getPlans()) {
			CoordI coord = getHomeCoord(plan);
			if (coord != null) {
				return coord;
			}
		}

		return null;
	}
}
