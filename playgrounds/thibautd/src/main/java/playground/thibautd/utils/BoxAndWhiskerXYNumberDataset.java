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
 * Multiple series are supported, but JFreeChart renderers are unable to 
 * display a multi-series box and whiskers XY plot properly.
 *
 * @author thibautd
 */
public class BoxAndWhiskerXYNumberDataset extends AbstractXYDataset implements BoxAndWhiskerXYDataset {
	static final long serialVersionUID = 1L;

	private final List<List<Tuple<Number, BoxAndWhiskerItem>>> items =
		new ArrayList<List<Tuple<Number, BoxAndWhiskerItem>>>();
	private final List<String> seriesKeys = new ArrayList<String>();

	// those are the (hard-coded) values of the BoxAndWhiskerCalculator
	private final double faroutCoefficient = 2d;
	private final double outlierCoefficient = 1.5d;

	// /////////////////////////////////////////////////////////////////////////
	// add methods
	// /////////////////////////////////////////////////////////////////////////
	public void setSeriesKey(final int series, final String key) {
		getItems(series);
		this.seriesKeys.set(series, key);
	}

	/**
	 * adds a box to the specified x value
	 *
	 * @param series the index of the series to modify
	 * @param xValue the xValue of the box
	 * @param item the item to add
	 */
	public void add(
			final int series,
			final Number xValue,
			final BoxAndWhiskerItem item) {
		List<Tuple<Number, BoxAndWhiskerItem>> seriesItems =
			getItems(series);
		seriesItems.add(new Tuple<Number, BoxAndWhiskerItem>(xValue, item));
	}

	/**
	 * adds a box to the specified x value
	 *
	 * @param series the index of the series to modify
	 * @param xValue the xValue of the box
	 * @param item the raw data from which getting the statistics
	 */
	public void add(
			final int series,
			final Number xValue,
			final List<? extends Number> item) {
		List<Tuple<Number, BoxAndWhiskerItem>> seriesItems =
			getItems(series);
		seriesItems.add(new Tuple<Number, BoxAndWhiskerItem>(
					xValue,
					BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(item)));
	}

	private List<Tuple<Number, BoxAndWhiskerItem>> getItems(final int series) {
		if (items.size() > series) {
		  return items.get(series);
		}
		else {
			List<Tuple<Number, BoxAndWhiskerItem>> list = null;

			for (int i=items.size(); i <= series; i++) {
				list = new ArrayList<Tuple<Number, BoxAndWhiskerItem>>();
				items.add(list);
				seriesKeys.add("series "+i);
			}

			return list;
		}
	}

	/**
	 * to use when only one series (adds to the series 0)
	 */
	public void add(final Number xValue, final BoxAndWhiskerItem item) {
		add(0, xValue, item);
	}

	/**
	 * to use when only one series (adds to the series 0)
	 */
	public void add(final Number xValue, final List<? extends Number> item) {
		add(0, xValue, item);
	}


	// /////////////////////////////////////////////////////////////////////////
	// interface methods
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public int getItemCount(final int series) {
		try {
			return this.items.get(series).size();
		}
		catch (IndexOutOfBoundsException e) {
			// the series does not exist
			return 0;
		}
	}

	@Override
	public Number getX(final int series, final int item) {
		return this.items.get(series).get(item).getFirst();
	}

	@Override
	public Number getY(final int series, final int item) {
		return this.getMeanValue(series, item);
	}

	@Override
	public double getFaroutCoefficient() {
		return this.faroutCoefficient;
	}

	@Override
	public Number getMaxOutlier(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getMaxOutlier();
	}

	@Override
	public Number getMaxRegularValue(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getMaxRegularValue();
	}

	@Override
	public Number getMeanValue(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getMean();
	}

	@Override
	public Number getMedianValue(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getMedian();
	}

	@Override
	public Number getMinOutlier(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getMinOutlier();
	}

	@Override
	public Number getMinRegularValue(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getMinRegularValue();
	}

	@Override
	public double getOutlierCoefficient() {
		return this.outlierCoefficient;
	}

	@Override
	public List getOutliers(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getOutliers();
	}

	@Override
	public Number getQ1Value(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getQ1();
	}

	@Override
	public Number getQ3Value(final int series, final int item) {
		return this.items.get(series).get(item).getSecond().getQ3();
	}

	@Override
	public int getSeriesCount() {
		return this.items.size();
	}

	/**
	 * @return the index of the series
	 */
	@Override
	public Comparable getSeriesKey(final int series) {
		return series;
	}
}

