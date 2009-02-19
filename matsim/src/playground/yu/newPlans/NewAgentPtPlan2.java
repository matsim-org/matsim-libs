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

import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;

/**
 * writes new Plansfile, in which every person will has 2 plans, one with type
 * "iv" and the other with type "oev", whose leg mode will be "pt" and who will
 * have only a blank <Route></Rout>
 * 
 * @author ychen
 * 
 */
public class NewAgentPtPlan2 extends NewPlan {
	private List<Plan> copyPlans = new ArrayList<Plan>();
	private List<BasicLeg.Mode> copyPlansModes = new ArrayList<BasicLeg.Mode>();

	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentPtPlan2(final Population plans) {
		super(plans);
		this.copyPlans.clear();
		this.copyPlansModes.clear();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(final Person person) {
		if (person.getLicense().equals("yes")) {
			// copyPlans: the copy of the plans.
			for (Plan pl : person.getPlans()) {
				Leg firstLeg = (Leg) pl.getActsLegs().get(1);
				BasicLeg.Mode legMode = firstLeg.getMode();
				// pl.setType(getPlanType(legMode));//????????????

				if (!legMode.equals(BasicLeg.Mode.car)) {
					Plan copyPlan = new Plan(person);
					// copyPlan.setType(Plan.Type.CAR);//????????????
					this.copyPlans.add(copyPlan);
					this.copyPlansModes.add(BasicLeg.Mode.car);
				} else if (!legMode.equals(BasicLeg.Mode.pt)) {
					Plan copyPlan = new Plan(person);
					// copyPlan.setType(Plan.Type.PT);//??????????????
					this.copyPlans.add(copyPlan);
					this.copyPlansModes.add(BasicLeg.Mode.pt);
				}

				List actsLegs = pl.getActsLegs();
				int actsLegsSize = actsLegs.size();
				for (
				// Plan copyPlan : this.copyPlans
				int j = 0; j > this.copyPlans.size(); j++) {
					Plan copyPlan = copyPlans.get(j);
					for (int i = 0; i < actsLegsSize; i++) {
						Object o = actsLegs.get(i);
						if (i % 2 == 0) {
							copyPlan.addAct((Act) o);
						} else {
							Leg leg = (Leg) o;
							Leg copyLeg = new Leg(leg);
							copyLeg.setRoute(null);
							copyLeg.setMode(this.copyPlansModes.get(j));
							// -----------------------------------------------
							// WITHOUT routeSetting!! traveltime of
							// BasicLeg.PTMODE or
							// BasicLeg.CARMODE can be calculated automaticly!!
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
		}
		this.pw.writePerson(person);
	}
	// static Plan.Type getPlanType(String mode) {
	// if (BasicLeg.MIVMODE.equalsIgnoreCase(mode))
	// return Plan.Type.CAR;
	// else if (BasicLeg.CARMODE.equalsIgnoreCase(mode))
	// return Plan.Type.CAR;
	// else if (BasicLeg.RIDEMODE.equalsIgnoreCase(mode))
	// return Plan.Type.UNDEFINED;
	// else if (BasicLeg.MOTORBIKEMODE.equalsIgnoreCase(mode))
	// return Plan.Type.UNDEFINED;
	// else if (BasicLeg.PTMODE.equalsIgnoreCase(mode))
	// return Plan.Type.PT;
	// else if (BasicLeg.TRAINMODE.equalsIgnoreCase(mode))
	// return Plan.Type.UNDEFINED;
	// else if (BasicLeg.BIKEMODE.equalsIgnoreCase(mode))
	// return Plan.Type.BIKE;
	// else if (BasicLeg.WALKMODE.equalsIgnoreCase(mode))
	// return Plan.Type.WALK;
	// else {
	// return Plan.Type.UNDEFINED;
	// }
	// }
}
