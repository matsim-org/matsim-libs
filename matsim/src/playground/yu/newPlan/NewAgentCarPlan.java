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

import java.util.List;

import org.matsim.basic.v01.BasicLeg;
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
public class NewAgentCarPlan extends NewPlan {
	private boolean haveCar;

	/**
	 * Constructor, writes file-head
	 * 
	 * @param plans
	 *            - a Plans Object, which derives from MATSim plansfile
	 */
	public NewAgentCarPlan(Population plans) {
		super(plans);
	}

	@Override
	public void run(Person person) {
		this.haveCar = false;
		for (Plan pl : person.getPlans()) {
			List actsLegs = pl.getActsLegs();
			a: {
				for (int i = 0; i < actsLegs.size(); i++) {
					Object o = actsLegs.get(i);
					if (i % 2 != 0) {
						if (((Leg) o).getMode().equals("car")) {
							this.haveCar = true;
							// pl.setType(Plan.Type.CAR);
							break a;
						}
					}
				}
			}
			if (this.haveCar) {
				for (int j = 0; j < actsLegs.size(); j++) {
					Object o = actsLegs.get(j);
					if (j % 2 != 0) {
						((Leg) o).setMode(BasicLeg.CARMODE);
					}
				}
			}
		}
		if (this.haveCar) {
			this.pw.writePerson(person);
		}
	}
}
