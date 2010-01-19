/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFacility2Link
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.balmermi.influenza.modules;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

public class PersonNoEducLessLeisure extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final Logger log = Logger.getLogger(PersonNoEducLessLeisure.class);

	private final Random random = MatsimRandom.getRandom();
	private double pctRemainingLeisure = 0.33;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonNoEducLessLeisure(double pctRemainingLeisure) {
		super();
		if ((pctRemainingLeisure < 0) || (pctRemainingLeisure > 1)) {
			log.warn("pctRemainingLeisure outside range, keeping default (pctRemainingLeisure="+this.pctRemainingLeisure+")");
		}
		else {
			this.pctRemainingLeisure = pctRemainingLeisure;
		}
		random.nextDouble();
	}

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		if (person.getPlans().size() != 1) { throw new RuntimeException("pid="+person.getId()+" must have one plan only!"); }
		Plan plan = person.getPlans().get(0);
		for (int i=1; i<plan.getPlanElements().size(); i=i+2) {
			LegImpl currLeg = (LegImpl)plan.getPlanElements().get(i);
			currLeg.setRoute(null);
		}
		ActivityImpl homeAct = (ActivityImpl)plan.getPlanElements().get(0);
		if (!homeAct.getType().equals("home")) { throw new RuntimeException("pid="+person.getId()+" first act is not home act!"); }
		for (int i=2; i<plan.getPlanElements().size(); i=i+2) {
			ActivityImpl currAct = (ActivityImpl)plan.getPlanElements().get(i);
			if (currAct.getType().startsWith("e")) {
				currAct.setType("home");
				currAct.setFacilityId(homeAct.getFacilityId());
				currAct.setLink(homeAct.getLink());
				currAct.setCoord(homeAct.getCoord());
			}
			else if (currAct.getType().startsWith("l")) {
				if (random.nextDouble() < pctRemainingLeisure) {
					currAct.setType("home");
					currAct.setFacilityId(homeAct.getFacilityId());
					currAct.setLink(homeAct.getLink());
					currAct.setCoord(homeAct.getCoord());
				}
			}
		}
	}
}
