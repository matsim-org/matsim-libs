/* *********************************************************************** *
 * project: org.matsim.*
 * PersonInitDemandSummaryTable.java
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

import java.util.ArrayList;

import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;

public class DoAndUndo extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private boolean doIt = true;
	private final ArrayList<String> leg_modes = new ArrayList<String>();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public DoAndUndo() {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private method
	//////////////////////////////////////////////////////////////////////

	private final void doIt(Person p) {
		if (!leg_modes.isEmpty()) { Gbl.errorMsg("Something is wrong!"); }
		Plan plan = p.getSelectedPlan();
		for (int i=1; i<plan.getActsLegs().size(); i=i+2) {
			Leg leg = (Leg)plan.getActsLegs().get(i);
			leg_modes.add(leg.getMode());
			leg.setMode("car");
		}
	}
	
	private final void undoIt(Person p) {
		if (leg_modes.isEmpty()) { Gbl.errorMsg("Something is wrong!"); }
		Plan plan = p.getSelectedPlan();
		for (int i=1; i<plan.getActsLegs().size(); i=i+2) {
			Leg leg = (Leg)plan.getActsLegs().get(i);
			leg.setMode(leg_modes.get((i-1)/2));
		}
		leg_modes.clear();
	}
	
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		if (doIt == true) {
			this.doIt(person);
			doIt = false;
		}
		else {
			this.undoIt(person);
			doIt = true;
		}
	}

	public void run(Plan plan) {
	}
}
