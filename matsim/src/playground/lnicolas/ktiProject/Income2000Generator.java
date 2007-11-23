/* *********************************************************************** *
 * project: org.matsim.*
 * Income2000Generator.java
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
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

/**
 * Associates household income information to persons, based on the average income
 * of the municipality the person lives in and the persons that live in the same household
 * as the person.
 * @author lnicolas
 *
 */
public class Income2000Generator {

	private ArrayList<Zone> municipalities;

	private TreeMap<IdI, MunicipalityInformation> municipalityInfo;

	public Income2000Generator(World world,
			TreeMap<IdI, MunicipalityInformation> mInfo) {
		TreeMap<IdI, Location> locations = ((ZoneLayer) world.getLayer("municipality")).getLocations();
		this.municipalityInfo = new TreeMap<IdI, MunicipalityInformation>();
		this.municipalities = new ArrayList<Zone>();
		// Remove those entries that contain invalid income 2000 information
		Iterator<Entry<IdI, MunicipalityInformation> > it = mInfo.entrySet().iterator();
		while (it.hasNext()) {
			Entry<IdI, MunicipalityInformation> entry = it.next();
			if (entry.getValue().getAvgIncome2000() >= 0) {
				this.municipalityInfo.put(entry.getKey(), entry.getValue());
				municipalities.add((Zone) locations.get(entry.getKey()));
			}
		}
		Gbl.random.nextInt();
	}

	/**
	 * @param persons A person at index {@code i} in {@code persons} must be member of
	 * the household in {@code households} at index {@code i}
	 * @param households A person at index {@code i} in {@code persons} must be member of
	 * the household in {@code households} at index {@code i}
	 */
	public void run(ArrayList<Person> persons, ArrayList<HouseholdI> households) {
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		for (int i = 0; i < persons.size(); i++) {
			run(persons.get(i), households.get(i));

			if (i % (persons.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		System.out.println();
	}

	/**
	 * Associates household income information to {@value person}, based on the average income
	 * of the municipality {@value person} lives in and the persons that live in the same
	 * household.
	 * @param person
	 * @param household
	 */
	public void run(Person person, HouseholdI household) {
		Zone zone = getHomeZone(person);

		household.setIncome(getIncome2000(person, zone));
	}

	public double getIncome2000(Person person, Zone zone) {
		MunicipalityInformation mInfo = municipalityInfo.get(zone.getId());
		return mInfo.getAvgIncome2000();
	}

	public double getIncome2000(Zone zone) {
		return municipalityInfo.get(zone.getId()).getAvgIncome2000();
	}

	protected Zone getHomeZone(Person person) {
		CoordI homeCoord = getHomeCoord(person);
		List<Zone> zones = getContainingZones(homeCoord);
		Zone zone = null;
		if (zones.size() == 0) {
			zone = getNearestZone(homeCoord);
		} else {
			// Get a random zone
			int zoneIndex = Gbl.random.nextInt(zones.size());
			zone = zones.get(zoneIndex);
		}
		return zone;
	}

	private Zone getNearestZone(CoordI homeCoord) {
		Zone nearestZone = null;
		double shortestDist = Double.POSITIVE_INFINITY;
		for (Zone zone : municipalities) {
			double dist = zone.getCenter().calcDistance(homeCoord);
			if (dist > 0 && dist < shortestDist) {
				nearestZone = zone;
				shortestDist = dist;
			}
		}
		return nearestZone;
	}

	protected ArrayList<Zone> getContainingZones(CoordI homeCoord) {
		ArrayList<Zone> zones = new ArrayList<Zone>();
		for (Zone zone : municipalities) {
			double dist = zone.calcDistance(homeCoord);
			if (dist == 0.0) {
				zones.add(zone);
			}
		}
		return zones;
	}

	protected static CoordI getHomeCoord(Person person) {
		// Get first plan (containing the home act)
		Plan homePlan = person.getPlans().get(0);
		// Get first act (containing the home coord)
		Act homeAct = (Act) homePlan.getActsLegs().get(0);
		return homeAct.getCoord();
	}

	/**
	 * Removes zones from {@value zones} for which there is no or invalid income
	 * information in {@value mInfo} and then maps persons to a random zone from the set
	 * of zones that contain its home acitivity coordinate.
	 * @param persons
	 * @param zones
	 * @param mInfo
	 * @return A list of zones where a person at index {@code i} in {@code persons} lives
	 * (have its home location) within the zone index {@code i} in the returned list of zones.
	 */
	public static ArrayList<Zone> mapPersonsToZones(
			ArrayList<Person> persons, ArrayList<Zone> zones,
			TreeMap<IdI, MunicipalityInformation> mInfo) {
		Iterator<Zone> it = zones.iterator();
		System.out.println("Nof zones before removing invalid income 2000 entries: "
				+ zones.size());
		while (it.hasNext()) {
			Zone zone = it.next();
			// Remove those entries that contain invalid income 2000
			// information
			if (mInfo.get(zone.getId()).getAvgIncome2000() < 0) {
				it.remove();
			}
		}
		System.out.println("Nof zones after removing invalid income 2000 entries: "
				+ zones.size());
		ArrayList<Zone> result = new ArrayList<Zone>();
		int i = 0;
		System.out.println("Mapping persons to zones...");
		String statusString = "|----------+-----------|";
		System.out.println(statusString);
		for (Person person : persons) {
			CoordI homeCoord = getHomeCoord(person);
			List<Zone> contZones = getContainingZones(homeCoord, zones);
			Zone zone = null;
			if (contZones.size() == 0) {
				zone = getNearestZone(homeCoord, zones);
			} else {
				// Get a random zone
				int zoneIndex = Gbl.random.nextInt(contZones.size());
				zone = contZones.get(zoneIndex);
			}
			result.add(zone);

			i++;
			if (i % (persons.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
		System.out.println("done");

		return result;
	}

	protected static ArrayList<Zone> getContainingZones(CoordI homeCoord,
			ArrayList<Zone> zones) {
		ArrayList<Zone> result = new ArrayList<Zone>();
		for (Zone zone : zones) {
			if (zone.contains(homeCoord)) {
				result.add(zone);
			}
		}
		return result;
	}

	protected static Zone getNearestZone(CoordI homeCoord, ArrayList<Zone> zones) {
		Zone nearestZone = null;
		double shortestDist = Double.POSITIVE_INFINITY;
		for (Zone zone : zones) {
			double dist = zone.calcDistance(homeCoord);
			if (dist > 0 && dist < shortestDist) {
				nearestZone = zone;
				shortestDist = dist;
			}
		}
		return nearestZone;
	}
}
