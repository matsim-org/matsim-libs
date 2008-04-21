/* *********************************************************************** *
 * project: org.matsim.*
 * Barbell2ndaryLocationGenerator.java
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
import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlanImpl;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.world.Zone;

/**
 * Looks for secondary locations for acts in a person's plan and assigns them the
 * coordinate of the respective facility. The primary acts and the home acts
 * must already contain coords (i.e. they must be assigned to a zone) in order for this
 * algorithm to work.
 * @author lnicolas
 *
 */
public class Barbell2ndaryLocationGenerator extends PersonAlgorithm {

	private GroupFacilitiesPerZone facilities;
	private ArrayList<Zone> referenceZones;

	TreeMap<Id, Zone> referenceZoneMap = new TreeMap<Id, Zone>();

	public Barbell2ndaryLocationGenerator(
			GroupFacilitiesPerZone facilities,
			ArrayList<Zone> referenceZones) {
		Gbl.random.nextInt();

		this.facilities = facilities;
		this.referenceZones = referenceZones;

		for (Zone z : referenceZones) {
			referenceZoneMap.put(z.getId(), z);
		}
	}

	public void run(ArrayList<Person> persons,
			ArrayList<Zone> homeZones, ArrayList<Zone> primaryActZones) {
		String statusString = "|----------+-----------|";
		System.out.println(statusString);

		for (int i = 0; i < persons.size(); i++) {
			boolean result = run(persons.get(i),
					homeZones.get(i), primaryActZones.get(i));
			if (result == false) {
				Gbl.errorMsg("No 2ndary location zone found for person "
						+ persons.get(i).getId());
			}

			if (i % (persons.size() / statusString.length()) == 0) {
				System.out.print(".");
				System.out.flush();
			}
		}
	}

	/**
	 * Chooses either the zone containing the home act of the given person's plan
	 * or the zone containing the primary act (of the given person's plan).
	 * @param person The person.
	 * @param homeZone The zone containing the home act of the given person's plan.
	 * @param primaryActZone The zone containing the primary act of the given person's plan.
	 * @return true if a zone for the secondary act could be found and assigned and false
	 * otherwise.
	 */
	private boolean run(Person person, Zone homeZone, Zone primaryActZone) {
		if (primaryActZone == null || Gbl.random.nextDouble() < 0.5) {
			return run(person, homeZone);
		}
		return run(person, primaryActZone);
	}

	/**
	 * For each act of the given person's plan that contains no coordinates, a random
	 * facility in the given {@code secondaryActZone} is taken and its coords are assigned
	 * to the coords of the current act.
	 * @param person The person.
	 * @param secondaryActZone The zone where the secondary activity is to be performed.
	 * @return true upon success, false if no facility for the given act could be found in
	 * {@code secondaryActZone}.
	 */
	public boolean run(Person person, Zone secondaryActZone) {
		Plan plan = person.getPlans().get(0);
		// Get several random facilities in random zones for the secondary locations
		BasicPlanImpl.ActIterator it = plan.getIteratorAct();
		while (it.hasNext()) {
			Act act = (Act) it.next();
			if (act.getCoord() == null
					|| (act.getCoord().getX() == 0
							&& act.getCoord().getY() == 0)) {
				// Get a random facility within this zone
				Facility fac = facilities.getRandomFacility(
						secondaryActZone.getId(), act.getType());
				if (fac == null) {
					Zone toZone = getNearestZone(secondaryActZone, act.getType());
					if (toZone == null) {
						Gbl.errorMsg("No facility for activity "
								+ act.getType()
								+ " found in Switzerland.");
					}
					fac = facilities.getRandomFacility(toZone.getId(), act.getType());
				}
				if (fac != null) {
					act.setCoord(fac.getCenter());
				} else {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Returns the nearest zone to the given {@code zone} that contains a facility that
	 * allows performing an act of the given {@code actType}.
	 * @param zone
	 * @param actType
	 * @return
	 */
	private Zone getNearestZone(Zone zone, String actType) {
		double nearestDist = Double.POSITIVE_INFINITY;
		Zone nearestZone = null;
		for (Zone zone2 : referenceZones) {
			if (facilities.containsActivityFacility(zone2.getId(), actType)) {
				double dist = zone.getCenter().calcDistance(zone2.getCenter());
				if (dist < nearestDist) {
					nearestDist = dist;
					nearestZone = zone2;
				}
			}
		}
		return nearestZone;
	}

	/**
	 * Assumes that the given person contains the Id of the home and the primary act
	 * zones in its knowledge, looks for these zones based on their Id and calls
	 * {@link #run(Person, Zone, Zone)}.
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	@Override
	public void run(Person person) {
		String desc = person.getKnowledge().getDesc();
		String[] zoneIds = desc.split(";");
		Zone homeZone = referenceZoneMap.get(new IdImpl(zoneIds[0]));
		Zone primActZone = null;
		if (zoneIds.length > 1) {
			primActZone = referenceZoneMap.get(new IdImpl(zoneIds[1]));
		}
		run(person, homeZone, primActZone);
	}
}
