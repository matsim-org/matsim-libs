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

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
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

	private static final NumberFormat kmFormat, percentFormat;
	
	static {

		kmFormat = NumberFormat.getInstance();
		kmFormat.setMaximumFractionDigits(1);
		percentFormat = NumberFormat.getPercentInstance();
		percentFormat.setMaximumFractionDigits(2);
		
	}
	
	private EnumMap<TransportMode, Frequency> frequencies = new EnumMap<TransportMode, Frequency>(TransportMode.class);
	private EnumMap<TransportMode, ResizableDoubleArray> rawData = new EnumMap<TransportMode, ResizableDoubleArray>(TransportMode.class);
	
	public void run(PersonImpl person) {
		this.run(person.getSelectedPlan());
	}

	public void run(PlanImpl plan) {

		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				TransportMode mode = leg.getMode();

				Frequency frequency = null;
				ResizableDoubleArray rawData = null;
				if (!this.frequencies.containsKey(mode)) {
					frequency = new Frequency();
					this.frequencies.put(mode, frequency);
					rawData = new ResizableDoubleArray();
					this.rawData.put(mode, rawData);
				} else {
					frequency = this.frequencies.get(mode);
					rawData = this.rawData.get(mode);
				}

				frequency.addValue(leg.getRoute().getDistance());
				rawData.addElement(leg.getRoute().getDistance());
			}
		}

	}

	public int getNumberOfModes() {
		return this.frequencies.keySet().size();
	}
	
	/**
	 * Returns the number of legs of a mode of a distance within a given range.
	 * 
	 * @param mode legs of which mode
	 * @param oneBound the one (usually lower) distance bound
	 * @param theOtherBound the other (usually the higher) distance bound
	 * @return
	 */
	public long getNumberOfLegs(TransportMode mode, double oneBound, double theOtherBound) {
		
		return 
		this.frequencies.get(mode).getCumFreq((oneBound > theOtherBound) ? oneBound : theOtherBound) - 
		this.frequencies.get(mode).getCumFreq((oneBound < theOtherBound) ? oneBound : theOtherBound);
		
	}
	
	/**
	 * 
	 * @param mode legs of which mode
	 * @return the overall frequency of legs of a mode
	 */
	public long getNumberOfLegs(TransportMode mode) {

		long numberOfLegs = 0;

		if (this.frequencies.containsKey(mode)) {
			numberOfLegs = this.frequencies.get(mode).getSumFreq();
		}

		return numberOfLegs;

	}

	/**
	 * 
	 * @param distanceClassIndex
	 * @return the number of legs in a distance class.
	 */
	public long getNumberOfLegs(double oneBound, double theOtherBound) {

		long numberOfLegs = 0;

		for (TransportMode mode : this.frequencies.keySet()) {
			numberOfLegs += this.getNumberOfLegs(mode, oneBound, theOtherBound);
		}

		return numberOfLegs;

	}

	/**
	 * @return the overall number of legs.
	 */
	public long getNumberOfLegs() {

		long numberOfLegs = 0;

		for (TransportMode mode : this.frequencies.keySet()) {
			numberOfLegs += this.getNumberOfLegs(mode);
		}

		return numberOfLegs;

	}

	public enum CrosstabFormat {ABSOLUTE, PERCENTAGE};

	/**
	 * Prints the crosstab.
	 *
	 * @param crosstabFormat indicates if absolute numbers or percentage of all legs are printed
	 * @param isCumulative indicates if cumulative numbers are printed
	 * @param distanceClasses the classification of distances
	 */
	public void printDistanceClasses(CrosstabFormat crosstabFormat, boolean isCumulative, double[] distanceClasses) {

		long numberOfLegs;
		
		System.out.println();
		/*
		 * header - start
		 */
		System.out.print("#i\td [km]");
		for (TransportMode mode : this.frequencies.keySet()) {
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
			for (TransportMode mode : this.frequencies.keySet()) {
				System.out.print("\t");
				if (isCumulative) {
					numberOfLegs = this.getNumberOfLegs(mode, Double.MIN_VALUE, distanceClasses[i]);
				} else {
					numberOfLegs = this.getNumberOfLegs(
							mode, 
							( (i == 0) ? Double.MIN_VALUE : distanceClasses[i - 1]), 
							distanceClasses[i]);
				}
				switch(crosstabFormat) {
				case ABSOLUTE:
					System.out.print(Long.toString(numberOfLegs));
					break;
				case PERCENTAGE:
					System.out.print(percentFormat.format((double) numberOfLegs / (double) this.getNumberOfLegs()));
					break;
				}
			}
			System.out.print("\t");
			if (isCumulative) {
				numberOfLegs = this.getNumberOfLegs(Double.MIN_VALUE, distanceClasses[i]);
			} else {
				numberOfLegs = this.getNumberOfLegs(
						( (i == 0) ? Double.MIN_VALUE : distanceClasses[i - 1]), 
						distanceClasses[i]);
			}
			switch(crosstabFormat) {
			case ABSOLUTE:
				System.out.print(Long.toString(numberOfLegs));
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
		for (TransportMode mode : this.frequencies.keySet()) {
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
			System.out.print(Long.toString(numberOfLegs));
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

	public void printDeciles(boolean isCumulative) {
		this.printQuantiles(isCumulative, 10);
	}
	
	/**
	 * @param crosstabFormat indicates if absolute numbers or percentage of all legs are printed
	 * @param isCumulative indicates if cumulative numbers are printed
	 * @param numberOfQuantiles number of quantiles desired
	 */
	public void printQuantiles(boolean isCumulative, int numberOfQuantiles) {

		System.out.println();

		/*
		 * header - start
		 */
		System.out.print("#p");
		for (TransportMode mode : this.frequencies.keySet()) {
			System.out.print("\t" + mode);
		}
		System.out.println();
		/*
		 * header - end
		 */
		
		/*
		 * table - start
		 */
		double[] quantiles = new double[numberOfQuantiles];
		for (int ii = 0; ii < numberOfQuantiles; ii++) {
			quantiles[ii] = ((double) ii + 1) / ((double) numberOfQuantiles);
		}
		
		for (int ii = 0; ii < numberOfQuantiles; ii++) {
			System.out.print(percentFormat.format(quantiles[ii]));
			for (TransportMode mode : this.frequencies.keySet()) {
				System.out.print("\t");
				System.out.print(kmFormat.format(StatUtils.percentile(this.rawData.get(mode).getInternalValues(), quantiles[ii] * 100.0)));
			}
			System.out.println();
		}
		/*
		 * table - end
		 */
	
		System.out.println();

	}
	
}
