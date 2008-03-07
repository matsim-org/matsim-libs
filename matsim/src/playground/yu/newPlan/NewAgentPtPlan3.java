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
public class NewAgentPtPlan3 extends PersonAlgorithm implements
		PersonAlgorithmI {
	/**
	 * internal writer, which can be used by object of subclass.
	 */
	protected PlansWriter pw;

	private List<Plan> copyPlans = new ArrayList<Plan>();

	/**
	 * Constructor, writes file-head
	 *
	 * @param plans -
	 *            a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentPtPlan3(final Plans plans) {
		this.copyPlans.clear();
		this.pw = new PlansWriter(plans);
		this.pw.writeStartPlans();
	}

	public void writeEndPlans() {
		this.pw.writeEndPlans();
	}

	@Override
	public void run(final Person person) {
		// if (person.getLicense().equals("yes")) {
		// copyPlans: the copy of the plans.
		for (Plan pl : person.getPlans()) {
			Leg firstLeg = (Leg) pl.getActsLegs().get(1);
			String legMode = firstLeg.getMode();
			pl.setType(NewAgentPtPlan2.getPlanType(legMode));

			if (!legMode.equals("car")) {
				if (person.getLicense().equals("yes")) {
					Plan copyPlan = new Plan(person);
					copyPlan.setType(Plan.Type.CAR);
					this.copyPlans.add(copyPlan);
				}
			} else if (!legMode.equals("pt")) {
				Plan copyPlan = new Plan(person);
				copyPlan.setType(Plan.Type.PT);
				this.copyPlans.add(copyPlan);
			}

			List actsLegs = pl.getActsLegs();
			int actsLegsSize = actsLegs.size();
			for (Plan copyPlan : this.copyPlans) {
				for (int i = 0; i < actsLegsSize; i++) {
					Object o = actsLegs.get(i);
					if (i % 2 == 0) {
						copyPlan.addAct((Act) o);
					} else {
						Leg leg = (Leg) o;
						Leg copyLeg = new Leg(leg);
						copyLeg.setRoute(null);
						copyLeg.setMode(copyPlan.getType().toString());
						// -----------------------------------------------
						// WITHOUT routeSetting!! traveltime of "pt" or
						// "car"can be calculated automaticly!!
						// -----------------------------------------------
						copyPlan.addLeg(copyLeg);
					}
				}
			}
		}

		for (Plan copyPlan : this.copyPlans) {
			person.addPlan(copyPlan);
		}
		this.copyPlans.clear();

		// }
		if (person.getLicense().equals("no")) {
			List<Plan> plans = person.getPlans();
			for (Plan pl : plans) {
				if (pl.getType().equals("car")) {
					plans.remove(pl);
				}
			}
		}

		this.pw.writePerson(person);
	}
}
