/* *********************************************************************** *
 * project: org.matsim.*
 * TwoCategoriesBoxAndWhiskerChart.java
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
package playground.thibautd.utils.charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.matsim.core.utils.charts.ChartUtil;

import java.awt.*;
import java.util.List;

/**
 * @author thibautd
 */
public class TwoCategoriesBoxAndWhiskerChart extends ChartUtil {
	private final DefaultBoxAndWhiskerCategoryDataset dataset =
		new DefaultBoxAndWhiskerCategoryDataset();
	private final DefaultStatisticalCategoryDataset errorBarsDataset =
		new DefaultStatisticalCategoryDataset();
	private final boolean plotStdDev;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////
	public TwoCategoriesBoxAndWhiskerChart(
			final String title,
			final String xAxisLabel,
			final String yAxisLabel,
			final boolean plotStdDev) {
		super(title, xAxisLabel, yAxisLabel);
		this.plotStdDev = plotStdDev;
		// log.warn( this.getClass().getSimpleName()+" cannot yet draw error bars" );
		// this.plotStdDev = false;
	}

	public TwoCategoriesBoxAndWhiskerChart(
			final String title,
			final String xAxisLabel,
			final String yAxisLabel) {
		this( title , xAxisLabel , yAxisLabel , false );
	}

	// /////////////////////////////////////////////////////////////////////////
	// specific public methods
	// /////////////////////////////////////////////////////////////////////////
	public void addItem(
			final List<? extends Number> itemData,
			final Comparable<?> rowKey,
			final Comparable<?> columnKey) {
		dataset.add( itemData , rowKey , columnKey );

		if (plotStdDev) {
			double average = 0;
			double stdDev = 0;
			int count = 0;

			for (Number num : itemData) {
				count++;
				average += num.doubleValue();
			}
			average /= count;

			double current;
			for (Number num : itemData) {
				current = num.doubleValue() - average;
				current *= current;
				stdDev += current;
			}
			stdDev /= count;
			stdDev = Math.sqrt( stdDev );

			errorBarsDataset.add( average , stdDev , rowKey , columnKey );
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// internal
	// /////////////////////////////////////////////////////////////////////////
	private void createChart() {
		this.chart = ChartFactory.createBoxAndWhiskerChart(
				chartTitle,
				xAxisLabel,
				yAxisLabel,
				dataset,
				true); // legend?

		if (plotStdDev) {
			CategoryPlot plot = chart.getCategoryPlot();
			// StatisticalLineAndShapeRenderer renderer =
			// 		new StatisticalLineAndShapeRenderer(
			// 			false,  // lines from average to average
			// 			false ); // shapes 
			StatisticalBarRenderer renderer =
				new NoLegendStatisticalBarRenderer();
			// do not draw bars
			renderer.setDrawBarOutline( false );
			renderer.setShadowVisible( false );
			renderer.setBasePaint( new Color( 0 , 0 , 0 , 0 ) );
			renderer.setAutoPopulateSeriesPaint( false );
			// draw all error bars in black
			renderer.setErrorIndicatorPaint( Color.BLACK );
			plot.setRenderer(
					1, // index
					renderer);
			plot.setDataset( 1 , errorBarsDataset );
			plot.setDatasetRenderingOrder( DatasetRenderingOrder.FORWARD );
		}

		this.addDefaultFormatting();
	}

	// /////////////////////////////////////////////////////////////////////////
	// interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public JFreeChart getChart() {
		if (this.chart == null) createChart();
		return this.chart;
	}
}

class NoLegendStatisticalBarRenderer extends StatisticalBarRenderer {
	private static final long serialVersionUID = 1L;

	@Override
	public LegendItem getLegendItem(
			final int datasetIndex,
			final int series) {
		return null;
	}
}
