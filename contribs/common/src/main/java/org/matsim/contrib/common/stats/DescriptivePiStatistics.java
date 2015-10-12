/* *********************************************************************** *
 * project: org.matsim.*
 * DescriptivePiStatistics.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.common.stats;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.UnivariateStatistic;
import org.apache.commons.math.stat.descriptive.rank.Max;
import org.apache.commons.math.stat.descriptive.rank.Min;
import org.apache.commons.math.stat.descriptive.rank.Percentile;
import org.apache.commons.math.util.ResizableDoubleArray;

/**
 * Descriptive statistics object where each sample has a pi-value (1/weight) assigned.
 * 
 * @author illenberger
 * 
 */
public class DescriptivePiStatistics extends DescriptiveStatistics {

	private ResizableDoubleArray piValues = new ResizableDoubleArray();

	private static final long serialVersionUID = 552953838961616624L;

	/**
	 * Creates a new descriptive statistics object initialized with dummy
	 * implementations that return {@link Double#NaN} (except
	 * min/max-implementations).
	 */
	public DescriptivePiStatistics() {
		DummyPiStatistics dummyStats = new DummyPiStatistics();
		setMeanImpl(dummyStats);
		setGeometricMeanImpl(dummyStats);
		setKurtosisImpl(dummyStats);
		setMaxImpl(new StatisticsWrapper(new Max()));
		setMinImpl(new StatisticsWrapper(new Min()));
		setPercentileImpl(new DummyPiPercentile());
		setSkewnessImpl(dummyStats);
		setVarianceImpl(dummyStats);
		setSumsqImpl(dummyStats);
		setSumImpl(dummyStats);
	}

	/**
	 * Adds a sample with pi=1.0.
	 */
	@Override
	public void addValue(double v) {
		addValue(v, 1.0);
	}

	/**
	 * Adds a sample with a pi-value.
	 * 
	 * @param v
	 *            a sample
	 * @param pi
	 *            the sample's pi-value
	 */
	public void addValue(double v, double pi) {
		if (windowSize != INFINITE_WINDOW) {
			if (getN() == windowSize) {
				piValues.addElementRolling(pi);
			} else if (getN() < windowSize) {
				piValues.addElement(pi);
			}
		} else {
			piValues.addElement(pi);
		}

		super.addValue(v);
	}

	/**
	 * Returns the pi-values (insertion-ordered).
	 * 
	 * @return the pi-values.
	 */
	public double[] getPiValues() {
		return piValues.getElements();
	}

	/**
	 * @see {@link DescriptiveStatistics#apply(UnivariateStatistic)}.
	 */
	@Override
	public double apply(UnivariateStatistic stat) {
		((UnivariatePiStatistic) stat).setPiValues(piValues.getInternalValues());
		return super.apply(stat);
	}

	/**
	 * @see {@link DescriptiveStatistics#clear()}.
	 */
	@Override
	public void clear() {
		piValues.clear();
		super.clear();
	}

	/**
	 * @see {@link DescriptiveStatistics#removeMostRecentValue()}.
	 */
	@Override
	public void removeMostRecentValue() {
		piValues.discardMostRecentElements(1);
		super.removeMostRecentValue();
	}

	private static class DummyPiStatistics implements UnivariatePiStatistic {

		@Override
		public void setPiValues(double[] piValues) {
			// does nothing
		}

		@Override
		public UnivariateStatistic copy() {
			return new DummyPiStatistics();
		}

		@Override
		public double evaluate(double[] values) {
			return Double.NaN;
		}

		@Override
		public double evaluate(double[] values, int begin, int length) {
			return Double.NaN;
		}
	}

	private static class StatisticsWrapper implements UnivariatePiStatistic {

		private UnivariateStatistic delegate;

		public StatisticsWrapper(UnivariateStatistic delegate) {
			this.delegate = delegate;
		}

		@Override
		public void setPiValues(double[] piValues) {
			// does nothing
		}

		@Override
		public UnivariateStatistic copy() {
			return new StatisticsWrapper(delegate);
		}

		@Override
		public double evaluate(double[] values) {
			return delegate.evaluate(values);
		}

		@Override
		public double evaluate(double[] values, int begin, int length) {
			return delegate.evaluate(values, begin, length);
		}
	}

	private static class DummyPiPercentile extends Percentile implements UnivariatePiStatistic {

		private static final long serialVersionUID = 3778314678555629077L;

		@Override
		public void setPiValues(double[] piValues) {
		}

		@Override
		public Percentile copy() {
			return new DummyPiPercentile();
		}

		@Override
		public double evaluate(double[] values, double p) {
			return Double.NaN;
		}

		@Override
		public double evaluate(double[] values, int begin, int length, double p) {
			return Double.NaN;
		}

		@Override
		public double evaluate(double[] values, int start, int length) {
			return Double.NaN;
		}

		@Override
		public double getQuantile() {
			return Double.NaN;
		}

		@Override
		public double evaluate(double[] values) {
			return Double.NaN;
		}

	}
}
