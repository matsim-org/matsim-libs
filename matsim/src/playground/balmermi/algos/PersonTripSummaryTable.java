/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTripSummaryTable.java
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.basic.v01.BasicPlan.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class PersonTripSummaryTable extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;
	//   0|  1|   2|        3| 4|    5|  6|   7|   8|   9|   10
	// miv|car|ride|motorbike|pt|train|bus|tram|bike|walk|undef
	private int [][] dist_cnt = new int[201][11];

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonTripSummaryTable(String outfile) {
		super();
		for (int i=0; i<201; i++) {
			for (int j=0; j<11; j++) {
				this.dist_cnt[i][j] = 0;
			}
		}
		System.out.println("    init " + this.getClass().getName() + " module...");
		try {
			fw = new FileWriter(outfile);
			out = new BufferedWriter(fw);
			out.write("dist\tmiv\tcar\tride\tmotorbike\tpt\ttrain\tbus\ttram\tbike\twalk\tundef\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// final method
	//////////////////////////////////////////////////////////////////////

	public final void close() {
		try {
			for (int i=0; i<201; i++) {
				out.write(Integer.toString(i));
				for (int j=0; j<11; j++) {
					out.write("\t" + this.dist_cnt[i][j]);
				}
				out.write("\n");
			}
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
		// plans
		Plan plan = person.getSelectedPlan();
		if (plan == null) { Gbl.errorMsg("Person id=" + person.getId() + "does not have a selected plan assigned!"); }
		LegIterator l_it = plan.getIteratorLeg();
		while (l_it.hasNext()) {
			Leg leg = (Leg)l_it.next();
			String mode = leg.getMode();
			double dist = leg.getRoute().getDist();
			dist = dist / 1000; // km
			int ii = (int)dist;
			if (ii > 200) { ii=200; }

			int jj = -1;
			if (mode.equals("miv")) { jj = 0; }
			else if (mode.equals("car")) { jj = 1; }
			else if (mode.equals("ride")) { jj = 2; }
			else if (mode.equals("motorbike")) { jj = 3; }
			else if (mode.equals("pt")) { jj = 4; }
			else if (mode.equals("train")) { jj = 5; }
			else if (mode.equals("bus")) { jj = 6; }
			else if (mode.equals("tram")) { jj = 7; }
			else if (mode.equals("bike")) { jj = 8; }
			else if (mode.equals("walk")) { jj = 9; }
			else if (mode.equals("undef")) { jj = 10; }
			else { Gbl.errorMsg("Person id=" + person.getId() + ": mode=" + mode + " not known!"); }
			
			this.dist_cnt[ii][jj]++;
		}
	}

	public void run(Plan plan) {
	}
}
