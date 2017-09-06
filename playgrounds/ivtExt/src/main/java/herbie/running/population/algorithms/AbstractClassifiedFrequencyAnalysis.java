/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractFrequencyAnalysis.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package herbie.running.population.algorithms;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.util.ResizableDoubleArray;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;

public abstract class AbstractClassifiedFrequencyAnalysis extends AbstractPersonAlgorithm {

	protected static final NumberFormat classFormat;
	protected static final NumberFormat percentFormat;

	static {
		classFormat = NumberFormat.getInstance();
		classFormat.setMaximumFractionDigits(1);
		percentFormat = NumberFormat.getPercentInstance();
		percentFormat.setMaximumFractionDigits(2);
	}

	private static final double DUMMY_NEGATIVE_BOUND = -1000.0;
	protected Map<String, Frequency> frequencies = new HashMap<String, Frequency>();
	protected Map<String, ResizableDoubleArray> rawData = new HashMap<String, ResizableDoubleArray>();

	public AbstractClassifiedFrequencyAnalysis(PrintStream out) {
		super();
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
	public long getNumberOfLegs(String mode, double oneBound, double theOtherBound) {
		return
		this.frequencies.get(mode).getCumFreq((oneBound > theOtherBound) ? oneBound : theOtherBound) -
		this.frequencies.get(mode).getCumFreq((oneBound < theOtherBound) ? oneBound : theOtherBound);
	}

	/**
	 *
	 * @param mode legs of which mode
	 * @return the overall frequency of legs of a mode
	 */
	public long getNumberOfLegs(String mode) {
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
		for (String mode : this.frequencies.keySet()) {
			numberOfLegs += this.getNumberOfLegs(mode, oneBound, theOtherBound);
		}
		return numberOfLegs;
	}

	/**
	 * @return the overall number of legs.
	 */
	public long getNumberOfLegs() {
		long numberOfLegs = 0;
		for (String mode : this.frequencies.keySet()) {
			numberOfLegs += this.getNumberOfLegs(mode);
		}
		return numberOfLegs;
	}

	public AbstractClassifiedFrequencyAnalysis() {
		super();
	}

	public enum CrosstabFormat {ABSOLUTE, PERCENTAGE}

	/**
	 * Prints the crosstab.
	 *
	 * @param crosstabFormat indicates if absolute numbers or percentage of all legs are printed
	 * @param isCumulative indicates if cumulative numbers are printed
	 * @param classes the classification of distances
	 */
	public void printClasses(CrosstabFormat crosstabFormat, boolean isCumulative, double[] classes, PrintStream out) {
		long numberOfLegs;
		out.println();
		/*
		 * header - start
		 */
		out.print("#i\tclass");
		for (String mode : this.frequencies.keySet()) {
			out.print("\t" + mode);
		}
		out.print("\tsum");
		out.println();
		/*
		 * header - end
		 */

		/*
		 * table - start
		 */
		for (int i=0; i < classes.length; i++) {
			long sumClass = 0;
			out.print(Integer.toString(i) + "\t");
			out.print(classFormat.format(classes[i]));
			for (String mode : this.frequencies.keySet()) {
				out.print("\t");
				if (isCumulative) {
					numberOfLegs = this.getNumberOfLegs(mode, DUMMY_NEGATIVE_BOUND, classes[i]);
				} else {
					numberOfLegs = this.getNumberOfLegs(
							mode,
							( (i == 0) ? DUMMY_NEGATIVE_BOUND : classes[i - 1]),
							classes[i]);
				}
				sumClass += numberOfLegs;

				switch(crosstabFormat) {
				case ABSOLUTE:
					out.print(Long.toString(numberOfLegs));
					break;
				case PERCENTAGE:
					out.print(percentFormat.format((double) numberOfLegs / (double) this.getNumberOfLegs()));
					break;
				}
			}
			out.print("\t");

			switch(crosstabFormat) {
			case ABSOLUTE:
				out.print(Long.toString(sumClass));
				break;
			case PERCENTAGE:
				out.print(percentFormat.format((double) sumClass / (double) this.getNumberOfLegs()));
				break;
			}
			out.println();
		}
		/*
		 * table - end
		 */

		/*
		 * sum - start
		 */
		out.print("#sum\t");
		for (String mode : this.frequencies.keySet()) {
			out.print("\t");
			numberOfLegs = this.getNumberOfLegs(mode);

			switch(crosstabFormat) {
			case ABSOLUTE:
				out.print(numberOfLegs);
				break;
			case PERCENTAGE:
				out.print(percentFormat.format((double) numberOfLegs / (double) this.getNumberOfLegs()));
				break;
			}
		}
		out.print("\t");
		numberOfLegs = this.getNumberOfLegs();

		switch(crosstabFormat) {
		case ABSOLUTE:
			out.print(Long.toString(numberOfLegs));
			break;
		case PERCENTAGE:
			out.print(percentFormat.format(1.0));
			break;
		}
		/*
		 * sum - end
		 */
		out.println();
	}

	public void printDeciles(boolean isCumulative, PrintStream out) {
		this.printQuantiles(isCumulative, 10, out);
	}

	/**
	 * @param crosstabFormat indicates if absolute numbers or percentage of all legs are printed
	 * @param isCumulative indicates if cumulative numbers are printed
	 * @param numberOfQuantiles number of quantiles desired
	 */
	public void printQuantiles(boolean isCumulative, int numberOfQuantiles, PrintStream out) {
		out.println();
		/*
		 * header - start
		 */
		out.print("#p");
		for (String mode : this.frequencies.keySet()) {
			out.print("\t" + mode);
		}
		out.println();
		/*
		 * header - end
		 */

		/*
		 * table - start
		 */
		double[] quantiles = new double[numberOfQuantiles];
		for (int ii = 0; ii < numberOfQuantiles; ii++) {
			quantiles[ii] = ((double) ii + 1) / (numberOfQuantiles);
		}

		for (int ii = 0; ii < numberOfQuantiles; ii++) {
			out.print(percentFormat.format(quantiles[ii]));
			for (String mode : this.frequencies.keySet()) {
				out.print("\t");
				out.print(classFormat.format(StatUtils.percentile(this.rawData.get(mode).getElements(), quantiles[ii] * 100.0)));
			}
			out.println();
		}
		/*
		 * table - end
		 */
		out.println();
	}
}