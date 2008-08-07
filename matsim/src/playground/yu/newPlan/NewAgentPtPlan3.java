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

import org.matsim.basic.v01.BasicPlan.Type;
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
public class NewAgentPtPlan3 extends NewPlan implements PersonAlgorithmI {

	private final List<Plan> copyPlans = new ArrayList<Plan>();

	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans -
	 *            a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentPtPlan3(final Population plans) {
		super(plans);
		copyPlans.clear();
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
					copyPlans.add(copyPlan);
				}
			} else if (!legMode.equals("pt")) {
				Plan copyPlan = new Plan(person);
				copyPlan.setType(Plan.Type.PT);
				copyPlans.add(copyPlan);
			}

			List actsLegs = pl.getActsLegs();
			int actsLegsSize = actsLegs.size();
			for (Plan copyPlan : copyPlans)
				for (int i = 0; i < actsLegsSize; i++) {
					Object o = actsLegs.get(i);
					if (i % 2 == 0)
						copyPlan.addAct((Act) o);
					else {
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

		for (Plan copyPlan : copyPlans)
			person.addPlan(copyPlan);
		copyPlans.clear();

		// }
		if (person.getLicense().equals("no")) {
			List<Plan> plans = person.getPlans();
			for (Plan pl : plans)
				if (pl.getType().equals(Type.CAR))
					plans.remove(pl);
		}

		pw.writePerson(person);
	}
}
