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

package org.matsim.plans.algorithms;

import java.util.ArrayList;

import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

public class PersonCreatePlanFromKnowledge extends PersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonCreatePlanFromKnowledge() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		Plan p = person.createPlan(null,null,"yes");
		Facility home_facility = person.getKnowledge().getActivities("home").get(0).getFacility();
		ArrayList<Activity> acts = person.getKnowledge().getActivities();

		try {
			// first act end time = [7am.9am]
			int time = 7*3600 + (Gbl.random.nextInt(2*3600));

			// first act (= home)
			p.createAct("home",home_facility.getCenter().getX(),home_facility.getCenter().getY(),home_facility.getLink(),0.0,time,time,false);
			p.createLeg(0,"car",time,0.0,time);

			int nof_acts = 1 + Gbl.random.nextInt(3);
			int dur = 12*3600/nof_acts;

			// in between acts
			for (int i=0; i<nof_acts; i++) {
				int act_index = Gbl.random.nextInt(acts.size());
				Activity act = acts.get(act_index);
				Facility f = act.getFacility();
				p.createAct(act.getType(),f.getCenter().getX(),f.getCenter().getY(),f.getLink(),time,(time+dur),dur,false);
				time += dur;
				p.createLeg(i+1,"car",time,0.0,time);
			}
			
			// last act (= home)
			p.createAct("home",home_facility.getCenter().getX(),home_facility.getCenter().getY(),home_facility.getLink(),time,(24*3600),(24*3600-time),false);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
