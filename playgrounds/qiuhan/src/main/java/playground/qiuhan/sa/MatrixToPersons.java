/* *********************************************************************** *
 * project: org.matsim.*
 * MatrixToPersons.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.qiuhan.sa;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

public class MatrixToPersons {

	private final Matrix m;
	private final Map<Id, Person> persons;
	private final Map<String, Coord> zoneIdCoords;
	private static String DUMMY = "dummy";
	private final Random random;
	private Set<String> legModes = null;
	private final NetworkImpl network;

	/**
	 * @param m
	 *            a Matrix in one time interval (e.g. hour)
	 * @param zoneIdCoords
	 */
	private MatrixToPersons(Matrix m, Map<String, Coord> zoneIdCoords,
			NetworkImpl network) {
		this.m = m;
		this.persons = new HashMap<Id, Person>();
		this.zoneIdCoords = zoneIdCoords;
		this.random = MatsimRandom.getRandom();
		this.network = network;
	}

	/**
	 * @param m
	 *            a Matrix in one time interval (e.g. hour)
	 * @param zoneIdCoords
	 * @param legModes
	 *            if legModes==null, the mode of legs will be automatically set
	 *            to "pseudo-pt"
	 */
	public MatrixToPersons(Matrix m, Map<String, Coord> zoneIdCoords,
			NetworkImpl network, Set<String> legModes) {

		this(m, zoneIdCoords, network);
		this.legModes = legModes;

	}

	public Map<Id, Person> createPersons() {
		for (Id from : this.m.getFromLocations().keySet()) {
			Coord fromZone = this.zoneIdCoords.get(from.toString());

			for (Entry entry : this.m.getFromLocEntries(from)) {
				Id toZoneId = entry.getToLocation();
				Coord toZone = this.zoneIdCoords.get(toZoneId.toString());

				int numberPersons = (int) entry.getValue();

				for (int i = 0; i < numberPersons; i++) {
					Id personId = new IdImpl(this.m.getId() + "-" + from + "-"
							+ toZoneId + "-" + i);
					createPerson(personId, fromZone, toZone);
				}
			}
		}
		return persons;
	}

	/**
	 * @param personId
	 * @param from
	 * @param to
	 */
	private void createPerson(Id personId, Coord from, Coord to) {
		Person per = new PersonImpl(personId);

		createPlans(per, from, to);

		this.persons.put(personId, per);
	}

	private void createPlans(Person per, Coord from, Coord to) {
		if (this.legModes != null && !this.legModes.isEmpty()) {
			for (String legMode : this.legModes) {
				per.addPlan(createPlan(legMode, from, to));
			}

		} else {
			per.addPlan(createPlan(TransportMode.car, from, to));
			// TODO with pseudo pt or not?
			// per.addPlan(createPlan(TransportMode.pt, from, to));
		}
	}

	private Plan createPlan(String legMode, Coord from, Coord to) {
		Plan plan = new PlanImpl();
		((PlanImpl) plan).setType(legMode);

		Activity firstAct = new ActivityImpl(DUMMY, from,
				XY2NearestPassableLink.getNearestPassableLink(from, network)
						.getId());
		int time = Integer.parseInt(this.m.getId());

		double endTime = (time - 1) * 3600 + this.random.nextDouble() * 3600d;
		firstAct.setEndTime(endTime);

		plan.addActivity(firstAct);

		Leg leg = new LegImpl(legMode);
		plan.addLeg(leg);

		Activity lastAct = new ActivityImpl(DUMMY, to, XY2NearestPassableLink
				.getNearestPassableLink(to, network).getId());
		plan.addActivity(lastAct);

		return plan;
	}
}
