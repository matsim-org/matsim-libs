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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonInitDemandSummaryTable extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private FileWriter fw = null;
	private BufferedWriter out = null;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonInitDemandSummaryTable(String outfile) {
		super();
		System.out.println("    init " + this.getClass().getName() + " module...");
		try {
			fw = new FileWriter(outfile);
			out = new BufferedWriter(fw);
//			out.write("pid\tsex\tage\tlicense\tcaravail\temployed\t" +
//			          "ticketcnt\ttickets\t" +
//			          "x\ty\t" +
//			          "actcnt\tactchain\tlegcnt\tmodes\n");
			out.write("pid\tsex\tage\tlicense\tcaravail\temployed\t" +
			          "ticketcnt\ttickets\t" +
			          "x\ty\n");
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
	public void run(Person person) {
		try {
			// person attributes
			out.write(person.getId() + "\t");
			out.write(person.getSex() + "\t");
			out.write(person.getAge() + "\t");
			out.write(person.getLicense() + "\t");
			out.write(person.getCarAvail() + "\t");
			out.write(person.getEmployed() + "\t");
			
			// travel cards
			TreeSet<String> tcs = person.getTravelcards();
			if (tcs.isEmpty()) {
				out.write("0" + "\t");
				out.write("null" + "\t");
			}
			else {
				out.write(tcs.size() + "\t");
				Iterator<String> tc_it = tcs.iterator();
				while (tc_it.hasNext()) {
					String tc = tc_it.next();
					out.write(tc + ",");
				}
				out.write("\t");
			}

			// knowledge
			// ignored

			// home coordinates
			Act home_act = (Act)person.getSelectedPlan().getActsLegs().get(0);
			out.write(home_act.getCoord().getX() + "\t");
			out.write(home_act.getCoord().getY() + "\t");

			// plans
//			Plan plan = person.getSelectedPlan();
//			if (plan != null) {
//				ArrayList<Object> actslegs = plan.getActsLegs();
//				int act_cnt = 0;
//				String actchain = "";
//				int leg_cnt = 0;
//				TreeSet<String> modes = new TreeSet<String>();
//				for (int i=0; i<actslegs.size(); i++) {
//					if (i%2 == 0) {
//						Act act = (Act)actslegs.get(i);
//						act_cnt++;
//						actchain = actchain + act.getType().charAt(0);
//					}
//					else {
//						Leg leg = (Leg)actslegs.get(i);
//						leg_cnt++;
//						modes.add(leg.getMode());
//					}
//				}
//				out.write(act_cnt + "\t");
//				out.write(actchain + "\t");
//				out.write(leg_cnt + "\t");
//				if (modes.isEmpty()) {
//					out.write("null");
//				}
//				else {
//					Iterator<String> mode_it = modes.iterator();
//					while (mode_it.hasNext()) {
//						String mode = mode_it.next();
//						out.write(mode + ",");
//					}
//				}
//			}
//			else {
//				out.write("0" + "\t");
//				out.write("-" + "\t");
//				out.write("0" + "\t");
//				out.write("-");
//			}
			out.write("\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void run(Plan plan) {
	}
}
