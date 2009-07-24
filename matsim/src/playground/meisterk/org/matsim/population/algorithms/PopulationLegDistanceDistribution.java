/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationLegDistanceDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.population.algorithms;

import java.util.HashMap;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Generates a crosstab of the absolute number of legs in a population, by leg mode and route distance.
 * Leg distances are classified.
 * Only selected plans are considered.
 * 
 * @author meisterk
 *
 */
public class PopulationLegDistanceDistribution implements PlanAlgorithm, PersonAlgorithm {

	public static final double[] distanceClasses = new double[]{0.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0, 100000.0, Double.MAX_VALUE};
	
	private HashMap<TransportMode, Integer[]> legDistanceDistribution = new HashMap<TransportMode, Integer[]>();
	
	public void run(PersonImpl person) {
		this.run(person.getSelectedPlan());
	}
	
	public void run(PlanImpl plan) {
		
		for (PlanElement pe : plan.getPlanElements()) {
			
			if (pe instanceof LegImpl) {
				
				LegImpl leg = (LegImpl) pe;
				TransportMode mode = leg.getMode();
				
				Integer[] distanceDistro = null;
				if (!this.legDistanceDistribution.containsKey(mode)) {
					distanceDistro = new Integer[distanceClasses.length];
					for (int ii=0; ii < distanceDistro.length; ii++) {
						distanceDistro[ii] = 0;
					}
					this.legDistanceDistribution.put(mode, distanceDistro);
				} else {
					distanceDistro = this.legDistanceDistribution.get(mode);
				}
				
				int index = getDistanceClassIndex(leg.getRoute().getDistance());
				distanceDistro[index]++;
			}
			
		}
		
	}

	public static int getDistanceClassIndex(double distance) {
		
		int index = 0;
		while (distance > distanceClasses[index]) {
			index++;
		}
		
		return index;
		
	}

	public HashMap<TransportMode, Integer[]> getLegDistanceDistribution() {
		return legDistanceDistribution;
	}

	public void printLegDistanceDistribution() {
		
		System.out.println();
		System.out.print("#mode");
		for (double d : distanceClasses) {
			System.out.print("\t" + Double.toString(d));
		}
		System.out.println();
		for (TransportMode mode : this.legDistanceDistribution.keySet()) {
			System.out.print(mode);
			for (Integer i : this.legDistanceDistribution.get(mode)) {
				System.out.print("\t" + Integer.toString(i));
			}
			System.out.println();
		}
		System.out.println();
		
	}
	
}
