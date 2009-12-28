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

import java.util.Random;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonBlurTimesPerTimeBin extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final int mutationRange;
	private final int binSize;
	
	private final Random rd = MatsimRandom.getLocalInstance();
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonBlurTimesPerTimeBin(int mutationRange, int binSize) {
		System.out.println("    init " + this.getClass().getName() + " module...");
		this.mutationRange = mutationRange;
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
		int now = 0;
		for (PlanElement e : plan.getPlanElements()) {
			if (e instanceof ActivityImpl) {
				ActivityImpl a = (ActivityImpl)e;
				if (!a.equals(((PlanImpl) plan).getLastActivity())) {
					a.setStartTime(now);
					int min = now;
					int endTime = (int)Math.round(a.getEndTime());
					int max = (((int)(endTime/binSize))+1)*binSize;
					if ((endTime-mutationRange) > min) {min = endTime-mutationRange; }
					if (((int)(endTime/binSize))*binSize > min) { min = ((int)(endTime/binSize))*binSize; }
					if ((endTime+mutationRange) < max) { max = endTime+mutationRange; }
					a.setEndTime(rd.nextInt(max-min)+min);
					now = (int)Math.round(a.getEndTime());
				}
			}
			else if (e instanceof LegImpl) {
				LegImpl l = (LegImpl)e;
				l.setDepartureTime(now);
				l.setArrivalTime(now);
			}
			else { throw new RuntimeException("Plan element type not known."); }
		}
	}
}
