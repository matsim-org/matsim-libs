/* *********************************************************************** *
 * project: org.matsim.*
 * PlansWriteTableForLoechl.java
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


/* *********************************************************************** *
 *               org.matsim.demandmodeling.plans.algorithms                *
 *                      PlansWriteTableForLoechl.java                      *
 *                          ---------------------                          *
 * copyright       : (C) 2006 by                                           *
 *                   Michael Balmer, Konrad Meister, Marcel Rieser,        *
 *                   David Strippgen, Kai Nagel, Kay W. Axhausen,          *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
 * email           : balmermi at gmail dot com                             *
 *                 : rieser at gmail dot com                               *
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PlansWriteTableForLoechl extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansWriteTableForLoechl() {
		super();
		try {
			fw = new FileWriter("output/table-for-loechl.txt");
			out = new BufferedWriter(fw);
			out.write("Pol_Nr\tKunde\tKunde_GA_dist\tGA\tGA_KuBe_dist\tKuBe\tKuBe_Kunde_dist\tKunde\tKunde_KuBe_dist\tKuBe\tKuBe_GA_dist\tGA\tGA_Kunde_dist\tKunde\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			out.flush();
			out.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(Person person) {
		int nofPlans = person.getPlans().size();

		try {
			out.write(person.getId().toString());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		for (int planId = 0; planId < nofPlans; planId++) {
			Plan plan = (Plan)person.getPlans().get(planId);
			try {
				handlePlan(plan);
			} catch (Exception e) {
				Gbl.warningMsg(this.getClass(), "run(Person)", "Skipping plan id="+planId + " of person id=" + person.getId() + " because of: " + e.getMessage());
			}
		}
	}

	public void run(Plan plan) {
		try {
			handlePlan(plan);
		} catch (Exception e) {
			Gbl.warningMsg(this.getClass(), "run(Person)", "Skipping plan id=unknown of person id=unknown because of: " + e.getMessage());
		}
	}

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	public void handlePlan(Plan plan) throws Exception {
		try {
			ArrayList actslegs = plan.getActsLegs();

			for (int i=0; i<actslegs.size()-2; i=i+2) {
				Act from_act = (Act)actslegs.get(i);
				Leg leg = (Leg)actslegs.get(i+1);

				out.write("\t" + from_act.getType());
				out.write("\t" + leg.getRoute().getDist());
			}
			Act last_act = (Act)actslegs.get(actslegs.size()-1);
			out.write("\t" + last_act.getType());
			out.write("\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
