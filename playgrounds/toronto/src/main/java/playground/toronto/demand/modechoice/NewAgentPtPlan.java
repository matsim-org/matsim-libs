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

package playground.toronto.demand.modechoice;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 *
 * @author ychen
 *
 */
public class NewAgentPtPlan extends NewPopulation {
	public NewAgentPtPlan(final Network network, final Population population, final String filename) {
		super(network, population, filename);
	}

	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			List<PlanImpl> copyPlans = new ArrayList<PlanImpl>();
			// copyPlans: the copy of the plans.
			for (Plan pl : person.getPlans()) {
				// set plan type for car, pt, walk
				((PlanImpl) pl).setType(TransportMode.car);
				PlanImpl ptPlan = new org.matsim.core.population.PlanImpl(person);
				ptPlan.setType(TransportMode.pt);
//				Plan walkPlan = new org.matsim.population.PlanImpl(person);
//				walkPlan.setType(Type.WALK);

				List<PlanElement> actsLegs = pl.getPlanElements();
				for (int i = 0; i < actsLegs.size(); i++) {
					PlanElement o = actsLegs.get(i);
					if (o instanceof Activity) {
						ptPlan.addActivity((Activity) o);
//						walkPlan.addAct((Act) o);
					} else if (o instanceof Leg) {
						Leg leg = (Leg) o;
						LegImpl ptLeg = new org.matsim.core.population.LegImpl((LegImpl) leg);
						ptLeg.setMode(TransportMode.pt);
						ptLeg.setRoute(null);
						// -----------------------------------------------
						// WITHOUT routeSetting!! traveltime of PT can be
						// calculated
						// automaticly!!
						// -----------------------------------------------
						ptPlan.addLeg(ptLeg);
						LegImpl walkLeg = new org.matsim.core.population.LegImpl((LegImpl) leg);
						walkLeg.setMode(TransportMode.walk);
						walkLeg.setRoute(null);
//						walkPlan.addLeg(walkLeg);
						if (!leg.getMode().equals(TransportMode.car)) {
							leg.setRoute(null);
							leg.setMode(TransportMode.car);
						}
					}
				}
				copyPlans.add(ptPlan);
//				copyPlans.add(walkPlan);
			}
			for (PlanImpl copyPlan : copyPlans) {
				person.addPlan(copyPlan);
			}
		}
		this.pw.writePerson(person);
	}
}
