/* *********************************************************************** *
 * project: org.matsim.*
 * XYLineHistogramDataset.java
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jfree.data.xy.AbstractIntervalXYDataset;

/**
 * A dataset meant to easily create either "line" or "plain" histograms.
 *<br>
 *
 * It allows to plot either histograms of cumulative distribution plots.
 *<br>
 * XXX currently unusable for "real" (plain) histograms!
 *
 * @author thibautd
 */
public class XYLineHistogramDataset extends AbstractIntervalXYDataset {
	private static final Log log =
		LogFactory.getLog(XYLineHistogramDataset.class);

	private static final long serialVersionUID = 1L;
	private boolean cumulative = false;

	private final double binWidth;

	private final List<Comparable> keys = new ArrayList<Comparable>();
	private final List<List<Double>> yValues = new ArrayList<List<Double>>();
	private final List<Double> xStartValues = new ArrayList<Double>();
	private final List<List<Double>> rawXValues = new ArrayList<List<Double>>();

	// ////////////////////////////////////////////////////////////////////////
	// constructor
	// ////////////////////////////////////////////////////////////////////////
	public XYLineHistogramDataset(final double binWidth) {
		this.binWidth = binWidth;
	}

	// ////////////////////////////////////////////////////////////////////////
	// data acquisition
	// ////////////////////////////////////////////////////////////////////////
	/**
	 * @param seriesKey the series identifier
	 * @param xValues the x values of the elements to count.
	 */
	public void addSeries(final Comparable seriesKey, final List<Double> xValues) {
		this.keys.add(seriesKey);

		List<Double> list = new ArrayList<Double>(xValues);
		this.rawXValues.add(list);
		List<Double> newSeries = new ArrayList<Double>();
		Collections.sort(list);

		this.xStartValues.add(list.get(0));
		double currentUpperBound = list.get(0) + binWidth;
		int currentCount = 0;
		double totalCount = list.size();
		
		for (double xValue : list) {
			if (xValue < currentUpperBound) {
				currentCount++;
			}
			else {
				newSeries.add(((double) currentCount) / totalCount);

				currentUpperBound += binWidth;
				while (xValue >= currentUpperBound) {
					newSeries.add(0d);
					currentUpperBound += binWidth;
				}
				currentCount = 1;
			}
		}

		this.yValues.add(Collections.unmodifiableList(newSeries));
		fireDatasetChanged();
	}

	/**
	 * @param value true if the plot must be a cumulative distribution plot, false
	 * if the plot must be an histogramm.
	 */
	public void setCumulative(final boolean value) {
		this.cumulative = value;
		fireDatasetChanged();
	}

	// ///////////////////////////////////////////////////////////////////////
	// interface methods
	// ///////////////////////////////////////////////////////////////////////
	@Override
	public int getItemCount(final int series) {
		return cumulative ? 2 * this.rawXValues.get(series).size() : 2 * this.yValues.get(series).size();
	}

	@Override
	public Number getY(final int series, final int item) {
		int index = item / 2;
		double value;

		if (cumulative) {
			value = (item / 2);
			value /= (double) this.rawXValues.get(series).size();
		}
		else {
			value = this.yValues.get(series).get(index);
		}

		return value;
	}

	@Override
	public Number getX(final int series, final int item) {
		if (cumulative) return this.rawXValues.get(series).get(item / 2);
		boolean even = ((double) item) % 2d == 0;
		//return this.xStartValues.get(series) + ( (item + 1) / 2 ) * binWidth;
		return even ? getStartX(series, item / 2) : getEndX(series, item / 2);
	}

	@Override
	public int getSeriesCount() {
		return this.yValues.size();
	}

	@Override
	public Comparable getSeriesKey(final int series) {
		return this.keys.get(series);
	}

	@Override
	public Number getEndX(final int series, final int item) {
		return this.xStartValues.get(series) + (item + 1) * binWidth;
	}

	@Override
	public Number getEndY(final int series, final int item) {
		return this.getY(series, item);
	}

	@Override
	public Number getStartX(final int series, final int item) {
		return this.xStartValues.get(series) + item * binWidth;
	}

	@Override
	public Number getStartY(final int series, final int item) {
		return 0d;
	}
}

