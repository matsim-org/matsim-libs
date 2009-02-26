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

package playground.yu.newPlans;

import java.util.ArrayList;
import java.util.List;

import org.matsim.interfaces.basic.v01.BasicLeg.Mode;
import org.matsim.interfaces.basic.v01.BasicPlan.Type;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.PersonAlgorithm;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewAgentPtPlan extends NewPlan implements PersonAlgorithm {
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentPtPlan(final Population plans) {
		super(plans);
	}

	public NewAgentPtPlan(final Population population, final String filename) {
		super(population, filename);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			List<Plan> copyPlans = new ArrayList<Plan>();
			// copyPlans: the copy of the plans.
			for (Plan pl : person.getPlans()) {
				// set plan type for car, pt, walk
				pl.setType(Type.CAR);
				Plan ptPlan = new org.matsim.population.PlanImpl(person);
				ptPlan.setType(Type.PT);
				Plan walkPlan = new org.matsim.population.PlanImpl(person);
				walkPlan.setType(Type.WALK);

				List actsLegs = pl.getActsLegs();
				for (int i = 0; i < actsLegs.size(); i++) {
					Object o = actsLegs.get(i);
					if (i % 2 == 0) {
						ptPlan.addAct((Act) o);
						walkPlan.addAct((Act) o);
					} else {
						Leg leg = (Leg) o;
						Leg ptLeg = new org.matsim.population.LegImpl(leg);
						ptLeg.setMode(Mode.pt);
						ptLeg.setRoute(null);
						// -----------------------------------------------
						// WITHOUT routeSetting!! traveltime of PT can be
						// calculated
						// automaticly!!
						// -----------------------------------------------
						ptPlan.addLeg(ptLeg);
						Leg walkLeg = new org.matsim.population.LegImpl(leg);
						walkLeg.setMode(Mode.walk);
						walkLeg.setRoute(null);
						walkPlan.addLeg(walkLeg);
						if (!leg.getMode().equals(Mode.car)) {
							leg.setRoute(null);
							leg.setMode(Mode.car);
						}
					}
				}
				copyPlans.add(ptPlan);
				copyPlans.add(walkPlan);
			}
			for (Plan copyPlan : copyPlans) {
				person.addPlan(copyPlan);
			}
		}
		this.pw.writePerson(person);
	}
}
