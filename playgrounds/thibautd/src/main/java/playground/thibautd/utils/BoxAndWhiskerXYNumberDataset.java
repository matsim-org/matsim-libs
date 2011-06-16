/* *********************************************************************** *
 * project: org.matsim.*
 * BoxAndWhiskerXYNumberDataset.java
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
import java.util.List;

import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerItem;
import org.jfree.data.statistics.BoxAndWhiskerXYDataset;
import org.jfree.data.xy.AbstractXYDataset;

import org.matsim.core.utils.collections.Tuple;

/**
 * Implementation of a {@link BoxAndWhiskerXYDataset} which allows for any
 * numerical X value.
 *
 * There is only one series.
 *
 * @author thibautd
 */
public class BoxAndWhiskerXYNumberDataset extends AbstractXYDataset implements BoxAndWhiskerXYDataset {
	static final long serialVersionUID = 1L;

	private boolean isSorted = true;
	private final List<Tuple<Number, BoxAndWhiskerItem>> items =
		new ArrayList<Tuple<Number, BoxAndWhiskerItem>>();

	// those are the (hard-coded) values of the BoxAndWhiskerCalculator
	private final double faroutCoefficient = 2d;
	private final double outlierCoefficient = 1.5d;

	// /////////////////////////////////////////////////////////////////////////
	// add methods
	// /////////////////////////////////////////////////////////////////////////
	public void add(final Number xValue, final BoxAndWhiskerItem item) {
		this.items.add(new Tuple<Number, BoxAndWhiskerItem>(xValue, item));
		this.isSorted = false;
	}

	public void add(final Number xValue, final List<? extends Number> item) {
		this.items.add(new Tuple<Number, BoxAndWhiskerItem>(
					xValue,
					BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(item)));
		this.isSorted = false;
	}


	// /////////////////////////////////////////////////////////////////////////
	// interface methods
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * @param series unused (only one series).
	 */
	@Override
	public int getItemCount(final int series) {
		return this.items.size();
	}

	/**
	 * @param series unused
	 * @param item the index of the item to get
	 */
	@Override
	public Number getX(final int series, final int item) {
		return this.items.get(item).getFirst();
	}

	/**
	 * @return the mean value
	 */
	@Override
	public Number getY(final int series, final int item) {
		return this.getMeanValue(series, item);
	}

	/**
	 * @return the value used to check if an outlier is farout
	 */
	@Override
	public double getFaroutCoefficient() {
		return this.faroutCoefficient;
	}

	/**
	 * @return normally, the max non farout value. here, no farout exclusion (yet)
	 */
	@Override
	public Number getMaxOutlier(final int series, final int item) {
		return this.items.get(item).getSecond().getMaxOutlier();
	}

	/**
	 * @return the maximum outlier of the item
	 */
	@Override
	public Number getMaxRegularValue(final int series, final int item) {
		return this.items.get(item).getSecond().getMaxRegularValue();
	}

	@Override
	public Number getMeanValue(final int series, final int item) {
		return this.items.get(item).getSecond().getMean();
	}

	@Override
	public Number getMedianValue(final int series, final int item) {
		return this.items.get(item).getSecond().getMedian();
	}

	@Override
	public Number getMinOutlier(final int series, final int item) {
		return this.items.get(item).getSecond().getMinOutlier();
	}

	@Override
	public Number getMinRegularValue(final int series, final int item) {
		return this.items.get(item).getSecond().getMinRegularValue();
	}

	@Override
	public double getOutlierCoefficient() {
		return this.outlierCoefficient;
	}

	@Override
	public List getOutliers(final int series, final int item) {
		return this.items.get(item).getSecond().getOutliers();
	}

	@Override
	public Number getQ1Value(final int series, final int item) {
		return this.items.get(item).getSecond().getQ1();
	}

	@Override
	public Number getQ3Value(final int series, final int item) {
		return this.items.get(item).getSecond().getQ3();
	}

	@Override
	public int getSeriesCount() {
		return 1;
	}

	/**
	 * @return null
	 */
	@Override
	public Comparable getSeriesKey(final int series) {
		return 1;
	}
}

