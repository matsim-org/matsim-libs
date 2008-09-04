/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLicenseModel.java
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

package playground.balmermi.census2000v2.modules;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.gbl.MatsimRandom;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.balmermi.census2000v2.data.CAtts;

public class PersonAssignPrimaryActivities extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignPrimaryActivities.class);
	private final ArrayList<String> list = new ArrayList<String>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignPrimaryActivities() {
		log.info("    init " + this.getClass().getName() + " module...");
		list.add(CAtts.ACT_W2); list.add(CAtts.ACT_W3);
		list.add(CAtts.ACT_EKIGA); list.add(CAtts.ACT_EPRIM);
		list.add(CAtts.ACT_ESECO); list.add(CAtts.ACT_EHIGH);
		list.add(CAtts.ACT_EOTHR);
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		this.run(person.getSelectedPlan());
	}

	public void run(Plan plan) {
		for (int i=0; i<plan.getActsLegs().size(); i=i+2) {
			String curr_type = ((Act)plan.getActsLegs().get(i)).getType();
			if (curr_type.equals(CAtts.ACT_HOME)) {
				((Act)plan.getActsLegs().get(i)).setPrimary(true);
			}
			else if (list.contains(curr_type)) {
				int j;
				for (j=i+2; j<plan.getActsLegs().size(); j=j+2) {
					if (!((Act)plan.getActsLegs().get(j)).getType().equals(curr_type)) { break; }
				}
				int r = 2*MatsimRandom.random.nextInt((j-i)/2);
				Act act = (Act)plan.getActsLegs().get(i+r);
				act.setPrimary(true);
				i=j-2;
			}
		}
	}
}
