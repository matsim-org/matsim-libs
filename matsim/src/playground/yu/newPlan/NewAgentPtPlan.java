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

import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PersonAlgorithmI;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 *
 * @author ychen
 *
 */
public class NewAgentPtPlan extends PersonAlgorithm implements PersonAlgorithmI {
	/**
	 * internal writer, which can be used by object of subclass.
	 */
	protected PlansWriter pw;

	/**
	 * Constructor, writes file-head
	 *
	 * @param plans -
	 *            a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentPtPlan(final Plans plans) {
		this.pw = new PlansWriter(plans);
		this.pw.writeStartPlans();
	}

	public void writeEndPlans() {
		this.pw.writeEndPlans();
	}

	@Override
	public void run(final Person person) {
		List<Plan> copyPlans = new ArrayList<Plan>();
		// copyPlans: the copy of the plans.
		for (Plan pl : person.getPlans()) {
			pl.setType("car");

			Plan copyPlan = new Plan(person);
			copyPlan.setType("pt");

			List actsLegs = pl.getActsLegs();
			for (int i = 0; i < actsLegs.size(); i++) {
				Object o = actsLegs.get(i);
				if (i % 2 == 0) {
					copyPlan.addAct((Act) o);
				} else {
					Leg leg = (Leg)o;
					if (!leg.getMode().equals("car")) {
						leg.setRoute(null);
						leg.setMode("car");
					}

					Leg copyLeg = new Leg((Leg) o);
					copyLeg.setMode("pt");
					// -----------------------------------------------
					// WITHOUT routeSetting!! traveltime of PT can be calculated
					// automaticly!!
					// -----------------------------------------------
					copyLeg.setRoute(null);
					copyPlan.addLeg(copyLeg);
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
