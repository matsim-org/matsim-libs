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

import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.basic.v01.BasicPlan.Type;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.algorithms.PersonAlgorithm;

import playground.yu.analysis.PlanModeJudger;

/**
 * writes new Plansfile, in which every person will has 3 plans, with type
 * "car", "pt" and "walk", whose leg mode will be "pt" or "walk" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewAgentWalkPlan extends NewPlan implements PersonAlgorithm {
	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentWalkPlan(final Population plans) {
		super(plans);
	}

	public NewAgentWalkPlan(final Population population, final String filename) {
		super(population, filename);
	}

	@Override
	public void run(final Person person) {
		if (Integer.parseInt(person.getId().toString()) < 1000000000) {
			List<Plan> copyPlans = new ArrayList<Plan>();
			// copyPlans: the copy of the plans.
			for (Plan pl : person.getPlans()) {
				// set plan type for car, pt, walk
				if (PlanModeJudger.usePt(pl)) {
					Plan walkPlan = new Plan(person);
					walkPlan.setType(Type.WALK);

					List actsLegs = pl.getActsLegs();
					for (int i = 0; i < actsLegs.size(); i++) {
						Object o = actsLegs.get(i);
						if (i % 2 == 0) {
							walkPlan.addAct((Act) o);
						} else {
							Leg leg = (Leg) o;
							// -----------------------------------------------
							// WITHOUT routeSetting!! traveltime of PT can be
							// calculated
							// automaticly!!
							// -----------------------------------------------
							Leg walkLeg = new Leg(Mode.walk);
							walkLeg.setNum(leg.getNum());
							walkLeg.setDepartureTime(leg.getDepartureTime());
							walkLeg.setTravelTime(leg.getTravelTime());
							walkLeg.setArrivalTime(leg.getArrivalTime());
							walkLeg.setRoute(null);
							walkPlan.addLeg(walkLeg);
							if (!leg.getMode().equals(Mode.car)) {
								leg.setRoute(null);
								leg.setMode(Mode.car);
							}
						}
					}
					copyPlans.add(walkPlan);
				}
			}
			for (Plan copyPlan : copyPlans) {
				person.addPlan(copyPlan);
			}
		}
		this.pw.writePerson(person);
	}
}
