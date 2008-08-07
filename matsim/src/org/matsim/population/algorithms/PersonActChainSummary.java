/* *********************************************************************** *
 * project: org.matsim.*
 * PersonActChainSummary.java
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

package org.matsim.population.algorithms;

import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;

public class PersonActChainSummary extends AbstractPersonAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	// TreeMap(String chain, Integer freq)
	private final TreeMap<String, Integer> chain_freq = new TreeMap<String, Integer>();

	// int[type][time]
	private static final int NOF_ACTS = 5;
	private static final int MAX_HOUR = 25;
	private int [][] durs = new int[NOF_ACTS][MAX_HOUR];

	// int[hour of day]
	private static final int MAX_START = 48;
	private int [] starts = new int[MAX_START];

	// int[time]
	private static final int MAX_PLANDUR = 48;
	private int [] plan_dur = new int[MAX_PLANDUR];

	// int[time]
	private static final int MAX_PLANCOMPDUR = 48;
	private int [] comp_plan_dur = new int[MAX_PLANCOMPDUR];

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonActChainSummary() {
		super();
		for (int i=0; i<NOF_ACTS; i++)
			for (int j=0; j<MAX_HOUR; j++)
				this.durs[i][j] = 0;

		for (int i=0; i<MAX_START; i++)
			this.starts[i] = 0;

		for (int i=0; i<MAX_PLANDUR; i++)
			this.plan_dur[i] = 0;

		for (int i=0; i<MAX_PLANCOMPDUR; i++)
			this.comp_plan_dur[i] = 0;
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		if (person.getPlans().size() != 1) {
			Gbl.errorMsg("[person_id=" + person.getId() + " does not have exactly one plan. not allowed.]");
		}

		// fill up chain_freq TreeMap
		String chain = "";
		Plan plan = person.getPlans().get(0);
		for (int i = 0; i < plan.getActsLegs().size(); i += 2) {
			Act act = (Act)plan.getActsLegs().get(i);
			chain = chain.concat(act.getType());
		}
		if (!this.chain_freq.containsKey(chain)) {
			this.chain_freq.put(chain, Integer.valueOf(0));
		}
		int freq = (this.chain_freq.remove(chain)).intValue();
		freq++;
		this.chain_freq.put(chain, Integer.valueOf(freq));

		// fill up the durations
		double dur_sum = 0;
		for (int a = 2; a < plan.getActsLegs().size()-2; a += 2) {
			Act act = (Act)plan.getActsLegs().get(a);
			double dur = act.getDur();
			dur_sum += dur;
			int j = (int)(dur/3600);
			if (j<0) { Gbl.errorMsg("[person_id=" + person.getId() +"; j=" + j + " something is wrong]"); }
			if (j>24) { j = 24; }
			int i = -1;
			if (act.getType().equals("h")) { i = 0; }
			else if (act.getType().equals("w")) { i = 1; }
			else if (act.getType().equals("e")) { i = 2; }
			else if (act.getType().equals("s")) { i = 3; }
			else if (act.getType().equals("l")) { i = 4; }
			else { Gbl.errorMsg("[person_id=" + person.getId() +"; act_type=" + act.getType() + " is not known]"); }
			this.durs[i][j]++;
		}
		int index = (int)(dur_sum/3600);
		this.plan_dur[index]++;

		// fill up the starts
		Act act = (Act)plan.getActsLegs().get(0);
		index = (int)(act.getEndTime()/3600);
		this.starts[index]++;

		// fill up the complete plan duration
		index = (int)((act.getEndTime() + dur_sum)/3600);
		this.comp_plan_dur[index]++;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void printChainFreq() {
		System.out.println("    printing chain frequencies of " + this.getClass().getName() + " algorithm...");
		Iterator c_it = this.chain_freq.keySet().iterator();
		while (c_it.hasNext()) {
			String chain = (String)c_it.next();
			Integer freq = this.chain_freq.get(chain);
			System.out.println("      " + chain + "\t" + freq);
		}
		System.out.println("    done.");
	}

	public final void printDurDists() {
		System.out.println("    printing duration distributions of " + this.getClass().getName() + " algorithm...");
		for (int i=0; i<NOF_ACTS; i++) {
			if (i==0) { System.out.println("      (in between) home duration distribution:"); }
			else if (i==1) { System.out.println("      work duration distribution:"); }
			else if (i==2) { System.out.println("      educ duration distribution:"); }
			else if (i==3) { System.out.println("      shop duration distribution:"); }
			else if (i==4) { System.out.println("      leis duration distribution:"); }
			for (int j=0; j<MAX_HOUR; j++) {
				System.out.println("        " + j + "-" + (j+1) + " hours\t" + this.durs[i][j]);
			}
		}
		System.out.println("    done.");
	}

	public final void printStartDists() {
		System.out.println("    printing start distributions of " + this.getClass().getName() + " algorithm...");
		for (int i=0; i<MAX_START; i++) {
			System.out.println("        " + i + "-" + (i+1) + " hours\t" + this.starts[i]);
		}
		System.out.println("    done.");
	}

	public final void printPlanDurDists() {
		System.out.println("    printing plan duration distributions of " + this.getClass().getName() + " algorithm...");
		for (int i=0; i<MAX_PLANDUR; i++) {
			System.out.println("        " + i + "-" + (i+1) + " hours\t" + this.plan_dur[i]);
		}
		System.out.println("    done.");
	}

	public final void printCompletePlanDurDists() {
		System.out.println("    printing complete plan duration distributions of " + this.getClass().getName() + " algorithm...");
		for (int i=0; i<MAX_PLANCOMPDUR; i++) {
			System.out.println("        " + i + "-" + (i+1) + " hours\t" + this.comp_plan_dur[i]);
		}
		System.out.println("    done.");
	}
}
