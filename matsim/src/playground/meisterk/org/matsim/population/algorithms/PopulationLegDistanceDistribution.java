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
import java.util.EnumMap;

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
		10000.0, 20000.0, 50000.0, 
		100000.0, 200000.0, 500000.0,
		1000000.0};

	private EnumMap<TransportMode, Integer[]> legDistanceDistribution = new EnumMap<TransportMode, Integer[]>(TransportMode.class);

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

	public int getNumberOfModes() {
		return this.legDistanceDistribution.keySet().size();
	}
	
	public int getNumberOfLegs(TransportMode mode, int distanceClassIndex) {
		return this.legDistanceDistribution.get(mode)[distanceClassIndex];
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

	public enum CrosstabFormat {ABSOLUTE, PERCENTAGE};

	/**
	 * Prints the crosstab.
	 *
	 * @param crosstabFormat indicates if absolute numbers or percentage of all legs are printed
	 * @param isCumulative indicates if cumulative numbers are printed
	 */
	public void printCrosstab(CrosstabFormat crosstabFormat, boolean isCumulative) {

		int numberOfLegs;
		
		NumberFormat kmFormat = NumberFormat.getInstance();
		kmFormat.setMaximumFractionDigits(1);
		
		NumberFormat percentFormat = NumberFormat.getPercentInstance();
		percentFormat.setMaximumFractionDigits(2);

		System.out.println();
		/*
		 * header - start
		 */
		System.out.print("#i\td [km]");
		for (TransportMode mode : this.legDistanceDistribution.keySet()) {
			System.out.print("\t" + mode);
		}
		System.out.print("\tsum");
		System.out.println();
		/*
		 * header - end
		 */

		/*
		 * table - start
		 */
		for (int i=0; i < distanceClasses.length; i++) {
			System.out.print(Integer.toString(i) + "\t");
			System.out.print(kmFormat.format(distanceClasses[i] / 1000));
			for (TransportMode mode : this.legDistanceDistribution.keySet()) {
				System.out.print("\t");
				if (isCumulative) {
					numberOfLegs = 0;
					for (int j=0; j<=i; j++) {
						numberOfLegs += this.getNumberOfLegs(mode, j);
					}
					
				} else {
					numberOfLegs = this.getNumberOfLegs(mode, i);
					
				}
				switch(crosstabFormat) {
				case ABSOLUTE:
					System.out.print(Integer.toString(numberOfLegs));
					break;
				case PERCENTAGE:
					System.out.print(percentFormat.format((double) numberOfLegs / (double) this.getNumberOfLegs()));
					break;
				}
			}
			System.out.print("\t");
			if (isCumulative) {
				numberOfLegs = 0;
				for (int j=0; j<=i; j++) {
					numberOfLegs += this.getNumberOfLegs(j);
				}
			} else {
				numberOfLegs = this.getNumberOfLegs(i);
			}
			switch(crosstabFormat) {
			case ABSOLUTE:
				System.out.print(Integer.toString(numberOfLegs));
				break;
			case PERCENTAGE:
				System.out.print(percentFormat.format((double) numberOfLegs / (double) this.getNumberOfLegs()));
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
		System.out.print("#sum\t");
		for (TransportMode mode : this.legDistanceDistribution.keySet()) {
			System.out.print("\t");
			numberOfLegs = this.getNumberOfLegs(mode);
			switch(crosstabFormat) {
			case ABSOLUTE:
				System.out.print(numberOfLegs);
				break;
			case PERCENTAGE:
				System.out.print(percentFormat.format((double) numberOfLegs / (double) this.getNumberOfLegs()));
				break;
			}
		}
		
		System.out.print("\t");
		numberOfLegs = this.getNumberOfLegs();
		switch(crosstabFormat) {
		case ABSOLUTE:
			System.out.print(Integer.toString(numberOfLegs));
			break;
		case PERCENTAGE:
			System.out.print(percentFormat.format(1.0));
			break;
		}
		/*
		 * sum - end
		 */

		System.out.println();

	}

}
