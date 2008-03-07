/* *********************************************************************** *
 * project: org.matsim.*
 * PlanSummary.java
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

package org.matsim.plans.algorithms;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.utils.misc.Time;

/**
 * Collects different statistical values on plans, activities and legs.
 *
 * @author mrieser
 */
public class PlanSummary extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////


	private int planCnt = 0;
	private int actCnt = 0;
	private final int nActTypes;
	private final int nLegModes;

	private final String[] actTypes;
	private final int[] actTypeCnt;
	private final double[] actTypeDurations;
	private final String[] legModes;
	private final int[] legModeCnt;
	private HashMap<Plan.Type, Integer> planTypes = new HashMap<Plan.Type, Integer>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlanSummary(final String[] activities, final String[] legmodes) {
		super();
		this.nActTypes = activities.length;
		this.nLegModes = legmodes.length;
		this.actTypes = new String[this.nActTypes];
		this.actTypeCnt = new int[this.nActTypes];
		this.actTypeDurations = new double[this.nActTypes];
		this.legModes = new String[this.nLegModes];
		this.legModeCnt = new int[this.nLegModes];

		init(activities, legmodes);
	}

	private final void init(final String[] activities, final String[] legmodes) {
		for (int i = 0; i < this.nActTypes; i++) {
			this.actTypeCnt[i] = 0;
			this.actTypeDurations[i] = 0;
			this.actTypes[i] = null;
		}
		for (int i = 0; i < this.nLegModes; i++) {
			this.legModeCnt[i] = 0;
			this.legModes[i] = null;
		}

		int max;
		// copy activities with unknown array-length into our own array
		if (activities.length < this.nActTypes) {
			max = activities.length;
		} else {
			max = this.nActTypes;
		}
		for (int i = 0; i < max; i++) {
			this.actTypes[i] = activities[i];
		}
		// copy legmodes with unknown array-length into our own array
		if (legmodes.length < this.nLegModes) {
			max = legmodes.length;
		} else {
			max = this.nLegModes;
		}
		for (int i = 0; i < max; i++) {
			this.legModes[i] = legmodes[i];
		}
		this.planTypes.clear();
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		for (int i=0; i<person.getPlans().size(); i++) {
			Plan plan = person.getPlans().get(i);
			run(plan);
		}
	}

	public void run(Plan plan) {
		int acts = 0;
		ArrayList<Object> actsLegs = plan.getActsLegs();
		for (int j=0; j<actsLegs.size(); j=j+2) {
			acts++;
			this.actCnt++;
			Act act = (Act)actsLegs.get(j);
			String actType = act.getType();
			double dur = act.getDur();
			int idx = getActTypeIndex(actType);
			if ((idx >= 0) && (dur >= 0)) {
				this.actTypeCnt[idx]++;
				this.actTypeDurations[idx] = this.actTypeDurations[idx] + dur;
			}
			if (j > 0) {
				Leg leg = (Leg)actsLegs.get(j-1);
				String legMode = leg.getMode();
				idx = getLegModeIndex(legMode);
				if ((idx >= 0) && (dur >= 0)) {
					this.legModeCnt[idx]++;
				}
			}
		}

		Integer count = this.planTypes.get(plan.getType());
		if (count == null) count = 0;
		this.planTypes.put(plan.getType(), count + 1);

		this.planCnt++;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private final int getActTypeIndex(String actType) {
		for (int i = 0; i < this.nActTypes; i++) {
			if (actType.equals(this.actTypes[i])) {
				return i;
			}
		}
		return -1;
	}

	private final int getLegModeIndex(String legMode) {
		for (int i = 0; i < this.nLegModes; i++) {
			if (legMode.equals(this.legModes[i])) {
				return i;
			}
		}
		return -1;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	public final void print() {
		System.out.println("----------------------------------------");
		System.out.println(this.getClass().getName() + ":");
		System.out.println("number of plans:       " + this.planCnt);
		System.out.println("number of activities:  " + this.actCnt);
		System.out.println("average # of act/plan: " + ((double)this.actCnt/(double)this.planCnt));
		// act summary
		for (int i = 0; i < this.nActTypes; i++) {
			String actType = this.actTypes[i];
			if (actType != null) {
				int count = this.actTypeCnt[i];
				double sum = this.actTypeDurations[i];
				if (count == 0) {
					System.out.println("activity '" + actType + "': no data available.");
				} else {
					System.out.println("activity '" + actType + "':");
					System.out.println("    count:         " + count);
					System.out.println("    avg. duration: " + Time.writeTime((sum / count)));
				}
			}
		}

		// leg summary
		System.out.println();
		for (int i = 0; i < this.nLegModes; i++) {
			String legMode = this.legModes[i];
			if (legMode != null) {
				int count = this.legModeCnt[i];
				if (count == 0) {
					System.out.println("leg mode '" + legMode + "': no data available.");
				} else {
					System.out.println("leg mode '" + legMode + "':");
					System.out.println("    count:         " + count);
				}
			}
		}
		// plan types summary
		System.out.println("\nplan types:");
		for (Plan.Type type : this.planTypes.keySet()) {
			System.out.println(type + " : " + this.planTypes.get(type));
		}
		System.out.println("----------------------------------------");
	}
}
