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

import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * collects different statistical values on plans, activities and legs
 */
public class PlanSummary extends PersonAlgorithm implements PlanAlgorithmI {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////


	private int planCnt_ = 0;
	private int actCnt_ = 0;
	private final int nActTypes_;
	private final int nLegModes_;
	
	private final String[] actTypes_;
	private final int[] actTypeCnt_;
	private final double[] actTypeDurations_;
	private final String[] legModes_;
	private final int[] legModeCnt_;
	private HashMap<String, Integer> planTypes = new HashMap<String, Integer>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlanSummary(final String[] activities, final String[] legmodes) {
		super();
		nActTypes_ = activities.length;
		nLegModes_ = legmodes.length;
		actTypes_ = new String[nActTypes_];
		actTypeCnt_ = new int[nActTypes_];
		actTypeDurations_ = new double[nActTypes_];
		legModes_ = new String[nLegModes_];
		legModeCnt_ = new int[nLegModes_];
		
		init(activities, legmodes);
	}

	private final void init(final String[] activities, final String[] legmodes) {
		for (int i = 0; i < nActTypes_; i++) {
			actTypeCnt_[i] = 0;
			actTypeDurations_[i] = 0;
			actTypes_[i] = null;
		}
		for (int i = 0; i < nLegModes_; i++) {
			legModeCnt_[i] = 0;
			legModes_[i] = null;
		}

		int max;
		// copy activities with unknown array-length into our own array
		if (activities.length < nActTypes_) {
			max = activities.length;
		} else {
			max = nActTypes_;
		}
		for (int i = 0; i < max; i++) {
			actTypes_[i] = activities[i];
		}
		// copy legmodes with unknown array-length into our own array
		if (legmodes.length < nLegModes_) {
			max = legmodes.length;
		} else {
			max = nLegModes_;
		}
		for (int i = 0; i < max; i++) {
			legModes_[i] = legmodes[i];
		}
		planTypes.clear();
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
			actCnt_++;
			Act act = (Act)actsLegs.get(j);
			String actType = act.getType();
			double dur = act.getDur();
			int idx = getActTypeIndex(actType);
			if (idx >= 0 && dur >= 0) {
				actTypeCnt_[idx]++;
				actTypeDurations_[idx] = actTypeDurations_[idx] + dur;
			}
			if (j > 0) {
				Leg leg = (Leg)actsLegs.get(j-1);
				String legMode = leg.getMode();
				idx = getLegModeIndex(legMode);
				if (idx >= 0 && dur >= 0) {
					legModeCnt_[idx]++;
				}
			}
		}
		
		String type = plan.getType();
		Integer count = planTypes.get(type);
		if (count == null) count = 0;
		planTypes.put(type, count + 1);
		
		planCnt_++;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final int getActTypeIndex(String actType) {
		for (int i = 0; i < nActTypes_; i++) {
			if (actType.equals(actTypes_[i])) {
				return i;
			}
		}
		return -1;
	}
	
	private final int getLegModeIndex(String legMode) {
		for (int i = 0; i < nLegModes_; i++) {
			if (legMode.equals(legModes_[i])) {
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
		System.out.println("number of plans:       " + planCnt_);
		System.out.println("number of activities:  " + actCnt_);
		System.out.println("average # of act/plan: " + ((double)actCnt_/(double)planCnt_));
		// act summary
		for (int i = 0; i < nActTypes_; i++) {
			String actType = actTypes_[i];
			if (actType != null) {
				int count = actTypeCnt_[i];
				double sum = actTypeDurations_[i];
				if (count == 0) {
					System.out.println("activity '" + actType + "': no data available.");
				} else {
					System.out.println("activity '" + actType + "':");
					System.out.println("    count:         " + count);
					System.out.println("    avg. duration: " + Gbl.writeTime((sum / count)));
				}
			}
		}

		// leg summary
		System.out.println();
		for (int i = 0; i < nLegModes_; i++) {
			String legMode = legModes_[i];
			if (legMode != null) {
				int count = legModeCnt_[i];
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
		for (String type : planTypes.keySet()) {
			System.out.println(type + " : " + planTypes.get(type));
		}
		System.out.println("----------------------------------------");
	}
}
