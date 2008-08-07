/* *********************************************************************** *
 * project: org.matsim.*
 * NewAgentPtPlan.java
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

package playground.yu.newPlan;

import java.util.ArrayList;
import java.util.List;

import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PersonAlgorithmI;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewAgentPtPlan extends NewPlan implements PersonAlgorithmI {
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans -
	 *            a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentPtPlan(final Population plans) {
		super(plans);
	}

	@Override
	public void run(final Person person) {
		List<Plan> copyPlans = new ArrayList<Plan>();
		// copyPlans: the copy of the plans.
		for (Plan pl : person.getPlans()) {
			pl.setType(Plan.Type.CAR);

			Plan copyPlan = new Plan(person);
			copyPlan.setType(Plan.Type.PT);

			List actsLegs = pl.getActsLegs();
			for (int i = 0; i < actsLegs.size(); i++) {
				Object o = actsLegs.get(i);
				if (i % 2 == 0) {
					copyPlan.addAct((Act) o);
				} else {
					Leg leg = (Leg) o;
					Leg copyLeg = new Leg(leg);
					copyLeg.setMode("pt");
					copyLeg.setRoute(null);
					// -----------------------------------------------
					// WITHOUT routeSetting!! traveltime of PT can be calculated
					// automaticly!!
					// -----------------------------------------------
					copyPlan.addLeg(copyLeg);
					if (!leg.getMode().equals("car")) {
						leg.setRoute(null);
						leg.setMode("car");
					}
				}
			}
			copyPlans.add(copyPlan);
		}
		for (Plan copyPlan : copyPlans) {
			person.addPlan(copyPlan);
		}
		this.pw.writePerson(person);
	}
}
