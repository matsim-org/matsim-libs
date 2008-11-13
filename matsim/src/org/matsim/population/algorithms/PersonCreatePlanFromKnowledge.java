/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCreatePlanFromKnowledge.java
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

package org.matsim.population.algorithms;

import java.util.ArrayList;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;

public class PersonCreatePlanFromKnowledge extends AbstractPersonAlgorithm {

	public PersonCreatePlanFromKnowledge() {
		super();
	}

	@Override
	public void run(final Person person) {
		Plan p = person.createPlan(true);
		Facility home_facility = person.getKnowledge().getActivities("home").get(0).getFacility();
		ArrayList<Activity> acts = person.getKnowledge().getActivities();

		// first act end time = [7am.9am]
		int time = 7*3600 + (MatsimRandom.random.nextInt(2*3600));

		// first act (= home)
		Act a = p.createAct("home", home_facility.getCenter());
		a.setLink(home_facility.getLink());
		a.setStartTime(0.0);
		a.setDur(time);
		a.setEndTime(time);
		a.setFacility(home_facility);
		Leg l = p.createLeg(BasicLeg.Mode.car);
		l.setDepartureTime(time);
		l.setTravelTime(0);
		l.setArrivalTime(time);

		int nof_acts = 1 + MatsimRandom.random.nextInt(3);
		int dur = 12*3600/nof_acts;

		// in between acts
		for (int i=0; i<nof_acts; i++) {
			int act_index = MatsimRandom.random.nextInt(acts.size());
			Activity act = acts.get(act_index);
			Facility f = act.getFacility();
			a = p.createAct(act.getType(),f.getCenter());
			a.setLink(f.getLink());
			a.setStartTime(time);
			a.setDur(dur);
			a.setEndTime(time + dur);
			a.setFacility(f);
			time += dur;
			l = p.createLeg(BasicLeg.Mode.car);
			l.setDepartureTime(time);
			l.setTravelTime(0);
			l.setArrivalTime(time);
		}

		// last act (= home)
		a = p.createAct("home",home_facility.getCenter());
		a.setLink(home_facility.getLink());
		a.setStartTime(time);
		a.setEndTime(24*3600);
		a.setDur(24*3600 - time);
		a.setFacility(home_facility);
	}
}
