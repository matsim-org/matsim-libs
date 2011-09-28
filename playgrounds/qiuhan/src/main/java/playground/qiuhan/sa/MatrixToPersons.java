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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

public class MatrixToPersons {
	// private Scenario s;
	//
	// public MatrixToPersons(Scenario s) {
	// this.s = s;
	// }

	private Matrix m;
	private Map<Id, Person> persons;
	private Map<Id, Coord> zoneIdCoords;
	private static String DUMMY = "dummy";
	private Random random;

	public MatrixToPersons(Matrix m, Map<Id, Coord> zoneIdCoords) {
		this.m = m;
		this.persons = new HashMap<Id, Person>();
		this.zoneIdCoords = zoneIdCoords;
		this.random = MatsimRandom.getRandom();
	}

	public Map<Id, Person> getPersons() {
		return persons;
	}

	private void createPersons() {
		for (Id from : this.m.getFromLocations().keySet()) {
			Coord fromZone = this.zoneIdCoords.get(from);

			for (Entry entry : this.m.getFromLocEntries(from)) {
				Id toZoneId = entry.getToLocation();
				Coord toZone = this.zoneIdCoords.get(toZoneId);

				int numberPersons = (int) (entry.getValue() + 0.5);

				for (int i = 0; i < numberPersons; i++) {
					Id personId = new IdImpl(from + "-" + toZoneId + "-" + i);
					createPerson(personId, fromZone, toZone);
				}
			}
		}
	}

	private void createPerson(Id personId, Coord from, Coord to) {
		Person per = new PersonImpl(personId);

		Plan plan = new PlanImpl();

		per.addPlan(plan);

		Activity firstAct = new ActivityImpl(DUMMY, from);
		int time = Integer.parseInt(this.m.getId());

		double endTime = (time - 1) * 3600 + this.random.nextDouble() * 3600d;
		firstAct.setEndTime(endTime);

		plan.addActivity(firstAct);

		Leg leg = new LegImpl(TransportMode.pt);
		plan.addLeg(leg);

		Activity lastAct = new ActivityImpl(DUMMY, to);
		plan.addActivity(lastAct);

		this.persons.put(personId, per);
	}
}
