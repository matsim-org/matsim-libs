/* *********************************************************************** *
 * project: org.matsim.*
 * PersonsSummaryTable.java
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

package playground.balmermi.census2000.modules;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonsSummaryTable extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static final String W = "w";
	private static final String E = "e";
	private static final String S = "s";
	private static final String L = "l";

	private FileWriter fw = null;
	private BufferedWriter out = null;

	private final int xsize = 8;
	private final int ysize = 16;
	private int [][] chain_ageempl = new int[xsize][ysize];

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonsSummaryTable(String outfile) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		for (int i=0; i<xsize; i++) {
			for (int j=0; j<ysize; j++) {
				chain_ageempl[i][j] = 0;
			}
		}
		try {
			fw = new FileWriter(outfile);
			out = new BufferedWriter(fw);
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
			out.write("\t-\tl\ts\tsl\te\tel\tes\tesl\tw\twl\tws\twsl\twe\twel\twes\twesl\n");
			for (int i=0; i<xsize; i++) {
				for (int j=0; j<ysize; j++) {
					if (j == 0) {
						if (i == 0) { out.write("0-5no"); }
						else if (i == 1) { out.write("0-5yes"); }
						else if (i == 2) { out.write("6-7no"); }
						else if (i == 3) { out.write("6-7yes"); }
						else if (i == 4) { out.write("8-65no"); }
						else if (i == 5) { out.write("8-65yes"); }
						else if (i == 6) { out.write(">65no"); }
						else if (i == 7) { out.write(">65yes"); }
					}
					out.write("\t" + this.chain_ageempl[i][j]);
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

	@Override
	public void run(Person pp) {
		PersonImpl person = (PersonImpl) pp;

		// calc i index
		int age = person.getAge();
		int i = -1;
		if (age < 6) { i = 0; }
		else if (age < 8) { i = 2; }
		else if (age < 66) { i = 4; }
		else { i = 6; }
		if (person.isEmployed()) { i++; }

		// calc j index
		Plan plan = person.getPlans().get(0);
		int l = 0;
		int s = 0;
		int e = 0;
		int w = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				if (L.equals(act.getType())) { l = 1; }
				if (S.equals(act.getType())) { s = 1; }
				if (E.equals(act.getType())) { e = 1; }
				if (W.equals(act.getType())) { w = 1; }
			}
		}
		int j = w*2*2*2 + e*2*2 + s*2 + l;

		this.chain_ageempl[i][j]++;
	}

	public void run(Plan plan) {
	}
}
