/* *********************************************************************** *
 * project: org.matsim.*
 * GroupFacilitiesPerZone.java
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
import java.util.Set;
import java.util.TreeMap;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.utils.collections.QuadTree;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Zone;

/**
 * Helper class (for performance purposes) that is used to
 * store facilities in QuadTrees grouped by Ids (like zone IDs) and therefore allows to find
 * the nearest facility to a given coordinate in a given zone relatively fast.
 * @author lnicolas
 *
 */
public class GroupFacilitiesPerZone {

	private TreeMap<String, TreeMap<IdI, ArrayList<Facility> > > facilitiesPerZonePerType =
		new TreeMap<String, TreeMap<IdI, ArrayList<Facility> > >();

	private TreeMap<IdI, QuadTree<Facility>> homeFacilitiesPerZone =
		new TreeMap<IdI, QuadTree<Facility>>();

	TreeMap<String, String> planActToFacActMapping = new TreeMap<String, String>();

	private ArrayList<Zone> zones;

	public final static String workActType = "work";

	public final static String shopActType = "shop";

	public final static String eduActType = "education";

	public final static String leisureActType = "leisure";

	public void run(ArrayList<Zone> zones, Facilities facilities) {
		for (String actType : PlansGenerator.actTypes) {
			facilitiesPerZonePerType.put(actType,
					new TreeMap<IdI,ArrayList<Facility>>());
		}

		planActToFacActMapping.put(PlansGenerator.workActType,
				GroupFacilitiesPerZone.workActType);
		planActToFacActMapping.put(PlansGenerator.leisureActType,
				GroupFacilitiesPerZone.leisureActType);
		planActToFacActMapping.put(PlansGenerator.eduActType,
				GroupFacilitiesPerZone.eduActType);
		planActToFacActMapping.put(PlansGenerator.shopActType,
				GroupFacilitiesPerZone.shopActType);
		planActToFacActMapping.put(PlansGenerator.homeActType,
				CommuterInformationGenerator.homeActType);

		getFacilitiesPerZone(facilities, zones);

		this.zones = zones;
	}

	private void getFacilitiesPerZone(
			Facilities facilities, ArrayList<Zone> zones) {

		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		int i = 0;
		for (Zone zone : zones) {
			Set<IdI> facilityIds = zone.getDownMapping().keySet();
			double maxX = Double.NEGATIVE_INFINITY;
			double maxY = Double.NEGATIVE_INFINITY;
			double minX = Double.POSITIVE_INFINITY;
			double minY = Double.POSITIVE_INFINITY;
			ArrayList<Facility> homeFacs = new ArrayList<Facility>();
			TreeMap<String, ArrayList<Facility> > facs =
				new TreeMap<String, ArrayList<Facility>>();
			for (String actType : PlansGenerator.actTypes) {
				facs.put(actType, new ArrayList<Facility>());
			}
			for (IdI facilityId : facilityIds) {
				Facility facility = (Facility) facilities.getLocation(facilityId);
				if (facility != null) {
					Activity activity = facility.getActivity(
							PersonToHomeFacilityMapper.homeActType);
					if (activity != null && activity.getCapacity() > 0) {
						homeFacs.add(facility);
						if (facility.getCenter().getX() < minX) {
							minX = facility.getCenter().getX();
						}
						if (facility.getCenter().getY() < minY) {
							minY = facility.getCenter().getY();
						}
						if (facility.getCenter().getX() > maxX) {
							maxX = facility.getCenter().getX();
						}
						if (facility.getCenter().getY() > maxY) {
							maxY = facility.getCenter().getY();
						}
						facs.get(PlansGenerator.homeActType).add(facility);
					}
					activity = facility.getActivity(
							GroupFacilitiesPerZone.workActType);
					if (activity != null && activity.getCapacity() > 0) {
						facs.get(PlansGenerator.workActType).add(facility);
					}
					activity = facility.getActivity(
							GroupFacilitiesPerZone.eduActType);
					if (activity != null && activity.getCapacity() > 0) {
						facs.get(PlansGenerator.eduActType).add(facility);
					}
					activity = facility.getActivity(
							GroupFacilitiesPerZone.leisureActType);
					if (activity != null && activity.getCapacity() > 0) {
						facs.get(PlansGenerator.leisureActType).add(facility);
					}
					activity = facility.getActivity(
							GroupFacilitiesPerZone.shopActType);
					if (activity != null && activity.getCapacity() > 0) {
						facs.get(PlansGenerator.shopActType).add(facility);
					}
				}
			}
			QuadTree<Facility> quadTree =
				new QuadTree<Facility>(minX, minY, maxX, maxY);
			for (Facility fac : homeFacs) {
				quadTree.put(fac.getCenter().getX(),
						fac.getCenter().getY(), fac);
			}

			if (quadTree.size() > 0) {
				homeFacilitiesPerZone.put(zone.getId(), quadTree);
			}
			for (String actType : PlansGenerator.actTypes) {
				if (facs.get(actType).size() > 0) {
					TreeMap<IdI, ArrayList<Facility> > facilitiesPerZone =
						facilitiesPerZonePerType.get(actType);
					facilitiesPerZone.put(zone.getId(), facs.get(actType));
				}
			}

			i++;
			if (i % (zones.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}

		System.out.println();
	}

	public Facility getNearestHomeFacility(IdI zoneId, CoordI coord) {
		QuadTree<Facility> facilities =
			homeFacilitiesPerZone.get(zoneId);
		if (facilities == null) {
			return null;
		}

		return facilities.get(coord.getX(), coord.getY());
	}

	public Zone getNearestZone(IdI zoneId, String planActType) {
		Zone zone = null;
		for (Zone z : zones) {
			if (z.getId().equals(zoneId)) {
				zone = z;
				break;
			}
		}
		double nearestDist = Double.POSITIVE_INFINITY;
		Zone nearestZone = null;
		for (Zone zone2 : zones) {
			if (containsActivityFacility(zone2.getId(), planActType)) {
				double dist = zone.getCenter().calcDistance(zone2.getCenter());
				if (dist < nearestDist) {
					nearestDist = dist;
					nearestZone = zone2;
				}
			}
		}
		return nearestZone;
	}

	public Facility getRandomFacility(IdI zoneId, String planActType) {
		ArrayList<Facility> facilities =
			facilitiesPerZonePerType.get(planActType).get(zoneId);
		if (facilities == null || facilities.size() == 0) {
			return null;
		}

		return getRandomFacility(facilities,
				planActToFacActMapping.get(planActType));
	}

	public boolean containsActivityFacility(IdI zoneId, String planActType) {
		ArrayList<Facility> facilities =
			facilitiesPerZonePerType.get(planActType).get(zoneId);
		if (facilities == null) {
			return false;
		}
		String facActType = planActToFacActMapping.get(planActType);
		for (Facility facility : facilities) {
			Activity activity = facility.getActivity(facActType);
			if (activity != null && activity.getCapacity() > 0) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns a random facility (distributed by the number of persons in each facility, i.e.
	 * it is more possible that a facility with a lot of persons is returned)
	 * within the given Location
	 * @param location
	 * @return a random facility
	 */
	private Facility getRandomFacility(ArrayList<Facility> facilities,
			String facActType) {
		ArrayList<Facility> facilityArray = new ArrayList<Facility>();
		ArrayList<Facility> unknownCapFacilityArray =
			new ArrayList<Facility>();
		int totalPersons = 0;
		for (Facility facility : facilities) {
			Activity activity = facility.getActivity(facActType);
			if (activity != null) {
				if (activity.getCapacity() < Integer.MAX_VALUE) {
					if (activity.getCapacity() > 0) {
						facilityArray.add(facility);
						totalPersons += activity.getCapacity();
					}
				} else {
					unknownCapFacilityArray.add(facility);
				}
			}
		}

		if (facilityArray.size() + unknownCapFacilityArray.size() == 0) {
			// Create a new facility
//			return createFacility(location, activityType);
//			Gbl.errorMsg("There are no facilities of type " + activityType
//					+ " in location " + location.getId());
			return null;
		}

		// Choose the resulting facility in 2 steps:
		// - First, choose between facilities that have an unlimited capacity
		// and the other facilities (that have a positive capacity)
		int facGroupIndex = Gbl.random.nextInt(facilityArray.size() +
				unknownCapFacilityArray.size());

		if (facGroupIndex < facilityArray.size()) {
			// Within the facilities with limited capacity, choose the
			// resulting facility according to their capacity distribution
			int facilityIndex = Gbl.random.nextInt(totalPersons);
			totalPersons = 0;
			for (Facility facility : facilityArray) {
				totalPersons += facility.getActivity(facActType).getCapacity();
				if (facilityIndex < totalPersons) {
					return facility;
				}
			}
		} else {
			// Within the facilities with unlimited capacity, choose a random
			// resulting facility (according to the normal distribution)
			int facilityIndex = Gbl.random.nextInt(unknownCapFacilityArray.size());
			return unknownCapFacilityArray.get(facilityIndex);
		}

		return null;
	}
}
