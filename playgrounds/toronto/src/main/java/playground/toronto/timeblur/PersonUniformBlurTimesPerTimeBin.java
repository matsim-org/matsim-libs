/* *********************************************************************** *
 * project: org.matsim.*
 * PersonBlurTimes.java
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

package playground.toronto.timeblur;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;

public class PersonUniformBlurTimesPerTimeBin extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final int binSize;

	private final Random rd = MatsimRandom.getLocalInstance();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonUniformBlurTimesPerTimeBin(int binSize) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.binSize = binSize;
		rd.nextInt();
		System.out.println("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(Person person) {
		for (Plan p : person.getPlans()) {
			this.run(p);
		}
	}

	public void run(Plan plan) {
		Map<Integer,ArrayList<Activity>> actBins = new TreeMap<Integer, ArrayList<Activity>>();
		Activity currAct = PopulationUtils.getFirstActivity( ((Plan) plan) );
		while (!currAct.equals(PopulationUtils.getLastActivity(((Plan) plan)))) {
			int endTime = (int)currAct.getEndTime();
			int binIndex = endTime/binSize;
			if (!actBins.containsKey(binIndex)) { actBins.put(binIndex,new ArrayList<Activity>()); }
			actBins.get(binIndex).add(currAct);
			final Activity act = currAct;
			currAct = PopulationUtils.getNextActivity(((Plan) plan), PopulationUtils.getNextLeg(((Plan) plan), act));
		}
		for (Integer binIndex : actBins.keySet()) {
			ArrayList<Activity> acts = actBins.get(binIndex);
			for (int i=0; i<acts.size(); i++) {
				Activity a = acts.get(i);
				a.setEndTime(binSize*((double)binIndex + (i + rd.nextDouble())/acts.size()));
			}
		}
	}
}
