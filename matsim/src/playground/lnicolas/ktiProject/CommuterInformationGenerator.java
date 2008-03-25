/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterInformationGenerator.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import org.matsim.basic.v01.BasicAct;
import org.matsim.basic.v01.BasicPlan;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;
import org.matsim.world.Zone;

/**
 * This class adds coordinates to the primary activities <i>act</i>
 * (the activities after the initial home activity) of a person, if they are of type
 * "w" or "e". The coordinates are determined as follows:
 * Based on the given {@code workCommuterMatrix} and {@code educationCommuterMatrix}, a zone
 * for the respective activity <i>act</i> is determined (based on the zone of the person's home activity),
 * then, a building that has capacity for <i>act</i> is determined and its coordinate
 * is associated to <i>act</i>.
 * @author lnicolas
 */
public class CommuterInformationGenerator extends PersonToHomeFacilityMapper {

	final String statusString = "|----------+-----------|";

	int maxFacilityId = 100000000;

	ArrayList<Location> primaryActLocations = new ArrayList<Location>();

	/**
	 * @param population A person at index {@code i} in {@code population} must live (have its home location) within
	 * the zone in {@code zones} at index {@code i}
	 * @param homeZones A person at index {@code i} in {@code population} must live (have its home location) within
	 * the zone in {@code zones} at index {@code i}
	 * @param facilities The facilities grouped by zones.
	 */
	public CommuterInformationGenerator(final ArrayList<Person> population,
			final ArrayList<Zone> homeZones, final GroupFacilitiesPerZone facilities) {
		super(population, homeZones, facilities);
		for (int i = 0; i < population.size(); i++) {
			this.primaryActLocations.add(null);
		}
	}

	/**
	 * First, the persons that have primary work activities are processed:
	 * The coordinates af these activities are set. Then,
	 * the persons that have primary education activities are processed.
	 * @param workCommuterMatrix
	 * @param educationCommuterMatrix
	 */
	public void run(final Matrix workCommuterMatrix,
			final Matrix educationCommuterMatrix) {
		System.out.println(this.statusString);

		setWorkFacilities(workCommuterMatrix);
		setEducationFacilities(educationCommuterMatrix);

		System.out.println();
	}

	private void setWorkFacilities(final Matrix workCommuterMatrix) {
		for (int i = 0; i < this.population.size(); i++) {
			Person person = this.population.get(i);
			Plan plan = person.getPlans().get(0);
			if (planContainsType(plan, PlansGenerator.workActType)) {
				setWorkFacility(i, workCommuterMatrix);
			}

			if (i % (this.population.size() * 2 / this.statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
	}

	private void setEducationFacilities(final Matrix educationCommuterMatrix) {
		for (int i = 0; i < this.population.size(); i++) {
			Person person = this.population.get(i);
			Plan plan = person.getPlans().get(0);
			if ((planContainsType(plan, PlansGenerator.workActType) == false)
					&& planContainsType(plan, PlansGenerator.eduActType)) {
				setEducationFacility(i, educationCommuterMatrix);
			}

			if (i % (this.population.size() * 2 / this.statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
	}

	private void setWorkFacility(final int index, final Matrix workCommuterMatrix) {
		Person person = this.population.get(index);
		Zone homeZone = this.zones.get(index);
		ArrayList<Entry> workZoneDistr
			= workCommuterMatrix.getFromLocEntries(homeZone);
		if (workZoneDistr == null) {
			Gbl.errorMsg("There are no work fromLocEntries for zone " + homeZone.getName() + " ("
					+ homeZone.getId() + ")");
		}
		Location workLocation = getRandomToZone(workZoneDistr);
		if (workLocation == null) {
			Gbl.errorMsg("There exists no to work zone in the commuter" +
					" matrix for zone " + homeZone.getName() + " (" + homeZone.getId() + ")");
		}
		Facility workFacility = this.facilities.getRandomFacility(
				workLocation.getId(),
				PlansGenerator.workActType);
//		if (workFacility == null) {
//			Zone z = facilities.getNearestZone(workLocation.getId(), PlansGenerator.workActType);
//			workFacility = facilities.getRandomFacility(z.getId(), PlansGenerator.workActType);
//			if (workFacility == null) {
//				System.out.println("There exist no work facilities in switzerland!!");
//				return;
//			}
//		}

		this.primaryActLocations.set(index, workLocation);
//		Knowledge knowledge =
//			person.createKnowledge("Created based on enterprise census of 2000");
//		knowledge.setDesc(knowledge.getDesc() + ";" + workLocation.getId());
//		ActivityFacilities actFac = knowledge.createActivityFacility(workActType);
//		actFac.addFacility(workFacility);

		PersonToHomeFacilityMapper.setActCoord(person, workFacility.getCenter(), PlansGenerator.workActType);
	}

	private void setEducationFacility(final int index,
			final Matrix educationCommuterMatrix) {
		Person person = this.population.get(index);
		Zone homeZone = this.zones.get(index);
		ArrayList<Entry> eduZoneDistr = educationCommuterMatrix.getFromLocEntries(homeZone);
		if (eduZoneDistr == null) {
			Gbl.errorMsg("There are no education fromLocEntries for zone " + homeZone.getName() + " ("
					+ homeZone.getId() + ")");
		}
		Location eduLocation = getRandomToZone(eduZoneDistr);
		if (eduLocation == null) {
			Gbl.errorMsg("There exists no to education zone for in the commuter" +
					" matrix for zone " + homeZone.getName() + " (" + homeZone.getId() + ")");
		}
		Facility eduFacility = this.facilities.getRandomFacility(eduLocation.getId(),
				PlansGenerator.eduActType);
//		if (eduFacility == null) {
//			Zone z = facilities.getNearestZone(eduLocation.getId(), PlansGenerator.eduActType);
//			eduFacility = facilities.getRandomFacility(z.getId(), PlansGenerator.eduActType);
//			if (eduFacility == null) {
//				System.out.println("There exist no education facilities in switzerland!!");
//				return;
//			}
//		}

		this.primaryActLocations.set(index, eduLocation);
//		Knowledge knowledge =
//			person.createKnowledge("Created based on enterprise census of 2000");
//		knowledge.setDesc(knowledge.getDesc() + ";" + eduLocation.getId());
//		ActivityFacilities actFac = knowledge.createActivityFacility(eduActType);
//		actFac.addFacility(eduFacility);

		PersonToHomeFacilityMapper.setActCoord(person, eduFacility.getCenter(), PlansGenerator.eduActType);
	}

//	private Facility createFacility(Location location, String activityType) {
//		Facility facility = getFacilityByCoord(location.getCenter(), location);
//		if (facility == null) {
//			ZoneLayer facilityLocations = (ZoneLayer) Gbl.getWorld().getLayer("facility");
//			String x = Double.toString(location.getCenter().getX());
//			String y = Double.toString(location.getCenter().getY());
//			String id = Integer.toString(maxFacilityId);
//			Zone zone = facilityLocations.createZone(id,
//					x, y, x, y, x, y, "0", "dummy created by " + this.getClass().getName());
//			facility = facilities.createFacility(id, x, y);
//			facility.setLocation(zone);
//			maxFacilityId++;
//		}
//		Activity act = facility.createActivity(activityType);
//		act.addOpentime(new Opentime("wk", "07:00:00", "18:00:00"));
//		act.setCapacity(1);
//
//		return facility;
//	}

//	private Facility getFacilityByCoord(Coord center, Location location) {
//		Set<IdI> facilityIds = location.getDownMapping().keySet();
//		for (IdI facilityId : facilityIds) {
//			Facility facility = this.facilities.getFacility(facilityId.asString());
//			if (facility.getCoord().equals(center)) {
//				return facility;
//			}
//		}
//
//		return null;
//	}

	public static Location getRandomToZone(final ArrayList<Entry> toZoneDistr) {
		if (toZoneDistr == null) {
			return null;
		}
		int entrySum = 0;
		for (Entry entry : toZoneDistr) {
			entrySum += entry.getValue();
		}
		if (entrySum == 0) {
			return null;
		}
		int toEntryIndex = Gbl.random.nextInt(entrySum);
		entrySum = 0;
		for (Entry entry : toZoneDistr) {
			entrySum += entry.getValue();
			if (toEntryIndex < entrySum) {
				return entry.getToLocation();
			}
		}

		return null;
	}

	private boolean planContainsType(final Plan plan, final String actType) {
		BasicPlan.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			BasicAct act = it.next();
			if (act.getType().equals(actType)) {
				return true;
			}
		}
		return false;
	}

	static public void writeCommuterDistribution(final String filename,
			final ArrayList<Zone> zones, final Facilities facilities, final Matrix commuterMatrix, final String actType) {
		BufferedWriter out;

		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		int i = 0;
		try {
			out = new BufferedWriter(new FileWriter(filename));
			out.write("GemNr\tcomCnt\tcapCnt\n");
			for (Zone zone : zones) {
				ArrayList<Entry> zoneDistr = commuterMatrix.getToLocEntries(zone);
				int commuterCount = 0;
				if (zoneDistr != null) {
					for (Entry entry : zoneDistr) {
						commuterCount += entry.getValue();
					}
				}

				int capacityCount = getCapacity(facilities, actType, zone);

				out.write(zone.getId() + "\t" + commuterCount + "\t"
						+ capacityCount + "\n");

				i++;
				if (i % (zones.size() / statusString.length()) == 0) {
					System.out.print(".");
					System.out.flush();
				}
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Error writing to file " + filename + ": "
					+ e.getMessage());
		}
		System.out.println("Commuter distribution written to " + filename);
	}

	public static int getCapacity(final Facilities facilities, final String actType, final Zone zone) {
		Set<IdI> facilityIds = zone.getDownMapping().keySet();
		int capacityCount = 0;
		for (IdI facilityId : facilityIds) {
			Facility facility = (Facility)facilities.getLocation(facilityId);
			if (facility != null) {
				Activity activity = facility.getActivity(actType);
				if (activity != null) {
					if (activity.getCapacity() >= 1000000000) {
						capacityCount = Math.max(activity.getCapacity(), capacityCount);
					} else if (capacityCount < 1000000000) {
						capacityCount += activity.getCapacity();
					}
				}
			}
		}
		return capacityCount;
	}

	public ArrayList<Location> getPrimaryActLocations() {
		return this.primaryActLocations;
	}
}
