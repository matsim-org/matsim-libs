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

import java.text.NumberFormat;
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

	public static final double[] distanceClasses = new double[]{
		0.0, 
		100.0,	200.0, 500.0, 
		1000.0, 2000.0, 5000.0, 
		10000.0, 20000.0, 50000.0, 100000.0, 
		Double.MAX_VALUE};
//	public static final DecimalFormat PERCENTAGE_PATTERN = new DecimalFormat("#.##"); 
	
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

	/**
	 * 
	 * @param mode
	 * @return the number of legs of a mode over all distance classes.
	 */
	public int getNumberOfLegs(TransportMode mode) {
		
		int numberOfLegs = 0;
		
		if (this.legDistanceDistribution.containsKey(mode)) {
			for (Integer i : this.legDistanceDistribution.get(mode)) {
				numberOfLegs += i;
			}
		}
		
		return numberOfLegs;
		
	}
	
	/**
	 * 
	 * @param distanceClassIndex
	 * @return the number of legs in a distance class.
	 */
	public int getNumberOfLegs(int distanceClassIndex) {

		int numberOfLegs = 0;
		
		for (TransportMode mode : this.legDistanceDistribution.keySet()) {
			numberOfLegs += this.legDistanceDistribution.get(mode)[distanceClassIndex];
		}
		
		return numberOfLegs;

	}
	
	/**
	 * @return the overall number of legs.
	 */
	public int getNumberOfLegs() {
		
		int numberOfLegs = 0;

		for (TransportMode mode : this.legDistanceDistribution.keySet()) {
			for (Integer i : this.legDistanceDistribution.get(mode)) {
				numberOfLegs += i;
			}
		}
		
		return numberOfLegs;
		
	}
	
	public enum CrosstabFormat{ABSOLUTE, PERCENTAGE};
	
	/**
	 * Prints the crosstab.
	 */
	public void printCrosstab(CrosstabFormat crosstabFormat) {
		
		NumberFormat nf = NumberFormat.getPercentInstance();
		nf.setMaximumFractionDigits(2);
		
		System.out.println();
		/*
		 * header - start
		 */
		System.out.println("#distance classes in km");
		System.out.print("#mode");
		for (int i=0; i < distanceClasses.length; i++) {
			if (i < (distanceClasses.length - 1)) {
				System.out.print("\t" + Double.toString(distanceClasses[i] / 1000));
			} else {
				System.out.print("\t>" + Double.toString(distanceClasses[i - 1] / 1000));
			}
		}
		System.out.print("\tsum");
		System.out.println();
		/*
		 * header - end
		 */

		/*
		 * table - start
		 */
		for (TransportMode mode : this.legDistanceDistribution.keySet()) {
			System.out.print(mode);
			for (Integer i : this.legDistanceDistribution.get(mode)) {
				System.out.print("\t");
				switch(crosstabFormat) {
				case ABSOLUTE:
					System.out.print(Integer.toString(i));
					break;
				case PERCENTAGE:
					System.out.print(nf.format((double) i / (double) this.getNumberOfLegs()));
					break;
				}
			}
			System.out.print("\t");
			switch(crosstabFormat) {
			case ABSOLUTE:
				System.out.print(Integer.toString(this.getNumberOfLegs(mode)));
				break;
			case PERCENTAGE:
				System.out.print(nf.format((double) this.getNumberOfLegs(mode) / (double) this.getNumberOfLegs()));
				break;
			}
			System.out.println();
		}
		/*
		 * table - end
		 */

		/*
		 * sum - start
		 */
		System.out.print("sum");
		for (int i=0; i < distanceClasses.length; i++) {
			System.out.print("\t");
			switch(crosstabFormat) {
			case ABSOLUTE:
				System.out.print(this.getNumberOfLegs(i));
				break;
			case PERCENTAGE:
				System.out.print(nf.format((double) this.getNumberOfLegs(i) / (double) this.getNumberOfLegs()));
				break;
			}
		}
		System.out.print("\t");
		switch(crosstabFormat) {
		case ABSOLUTE:
			System.out.print(Integer.toString(this.getNumberOfLegs()));
			break;
		case PERCENTAGE:
			System.out.print(nf.format((double) this.getNumberOfLegs() / (double) this.getNumberOfLegs()));
			break;
		}
		/*
		 * sum - end
		 */

		System.out.println();
		
	}
	
}
