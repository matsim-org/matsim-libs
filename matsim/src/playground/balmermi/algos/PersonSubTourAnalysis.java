/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSubTourAnalysis.java
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

package playground.balmermi.algos;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PersonSubTourAnalysis extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonSubTourAnalysis() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private method
	//////////////////////////////////////////////////////////////////////

	private final void handleSubTour(Plan plan, int start, int i, int j, int end) {
		System.out.println("[" + start + "," + i + "]&[" + j + "," + end + "]");
	}

	private final void extractSubTours(Plan plan, int start, int end) {
		boolean is_leaf = true;
		for (int i_act=start+2; i_act<end-1; i_act=i_act+2) {
			Act acti = (Act)plan.getActsLegs().get(i_act);
			for (int j_act=end-2; j_act>i_act; j_act=j_act-2) {
				Act actj = (Act)plan.getActsLegs().get(j_act);
				if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
				    (acti.getCoord().getY() == actj.getCoord().getY())) {
					// subtour found: start..i & j..end
					is_leaf = false;
					this.handleSubTour(plan,start,i_act,j_act,end);

					// next recursive step
					int ii_act = i_act;
					Act actii = acti;
					for (int jj_act=i_act+2; jj_act<=jj_act; jj_act=jj_act+2) {
						Act actjj = (Act)plan.getActsLegs().get(jj_act);
						if ((acti.getCoord().getX() == actj.getCoord().getX()) &&
						    (acti.getCoord().getY() == actj.getCoord().getY())) {
							this.extractSubTours(plan,ii_act,jj_act);
							ii_act = jj_act;
							actii = (Act)plan.getActsLegs().get(ii_act);
						}
					}
					return;
				}
			}
		}
		if (is_leaf) {
			// leaf-sub-tour: start..end
			this.handleSubTour(plan,start,end,end,end);
			// TODO balmermi: check if this is all right
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan == null) { Gbl.errorMsg("Person id=" + person.getId() + "does not have a selected plan."); }
		this.run(plan);
	}

	public void run(Plan plan) {
		System.out.println("----------------------------------------");
		System.out.println("pid=" + plan.getPerson().getId() + ":");
		for (int i=0; i<plan.getActsLegs().size(); i=i+2) {
			System.out.println("  " + i + ": " + ((Act)plan.getActsLegs().get(i)).getCoord());
		}
		this.extractSubTours(plan,0,plan.getActsLegs().size()-1);
		System.out.println("----------------------------------------");
	}
}
