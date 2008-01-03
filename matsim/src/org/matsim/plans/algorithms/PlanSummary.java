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
	private HashMap<String, Integer> planTypes = new HashMap<String, Integer>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlanSummary(final String[] activities, final String[] legmodes) {
		super();
		nActTypes = activities.length;
		nLegModes = legmodes.length;
		actTypes = new String[nActTypes];
		actTypeCnt = new int[nActTypes];
		actTypeDurations = new double[nActTypes];
		legModes = new String[nLegModes];
		legModeCnt = new int[nLegModes];
		
		init(activities, legmodes);
	}

	private final void init(final String[] activities, final String[] legmodes) {
		for (int i = 0; i < nActTypes; i++) {
			actTypeCnt[i] = 0;
			actTypeDurations[i] = 0;
			actTypes[i] = null;
		}
		for (int i = 0; i < nLegModes; i++) {
			legModeCnt[i] = 0;
			legModes[i] = null;
		}

		int max;
		// copy activities with unknown array-length into our own array
		if (activities.length < nActTypes) {
			max = activities.length;
		} else {
			max = nActTypes;
		}
		for (int i = 0; i < max; i++) {
			actTypes[i] = activities[i];
		}
		// copy legmodes with unknown array-length into our own array
		if (legmodes.length < nLegModes) {
			max = legmodes.length;
		} else {
			max = nLegModes;
		}
		for (int i = 0; i < max; i++) {
			legModes[i] = legmodes[i];
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
			actCnt++;
			Act act = (Act)actsLegs.get(j);
			String actType = act.getType();
			double dur = act.getDur();
			int idx = getActTypeIndex(actType);
			if (idx >= 0 && dur >= 0) {
				actTypeCnt[idx]++;
				actTypeDurations[idx] = actTypeDurations[idx] + dur;
			}
			if (j > 0) {
				Leg leg = (Leg)actsLegs.get(j-1);
				String legMode = leg.getMode();
				idx = getLegModeIndex(legMode);
				if (idx >= 0 && dur >= 0) {
					legModeCnt[idx]++;
				}
			}
		}
		
		String type = plan.getType();
		Integer count = planTypes.get(type);
		if (count == null) count = 0;
		planTypes.put(type, count + 1);
		
		planCnt++;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////
	
	private final int getActTypeIndex(String actType) {
		for (int i = 0; i < nActTypes; i++) {
			if (actType.equals(actTypes[i])) {
				return i;
			}
		}
		return -1;
	}
	
	private final int getLegModeIndex(String legMode) {
		for (int i = 0; i < nLegModes; i++) {
			if (legMode.equals(legModes[i])) {
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
		System.out.println("number of plans:       " + planCnt);
		System.out.println("number of activities:  " + actCnt);
		System.out.println("average # of act/plan: " + ((double)actCnt/(double)planCnt));
		// act summary
		for (int i = 0; i < nActTypes; i++) {
			String actType = actTypes[i];
			if (actType != null) {
				int count = actTypeCnt[i];
				double sum = actTypeDurations[i];
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
		for (int i = 0; i < nLegModes; i++) {
			String legMode = legModes[i];
			if (legMode != null) {
				int count = legModeCnt[i];
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
