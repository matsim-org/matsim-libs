/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.singapore.calibration.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;

/**
 * Add second y-Axis to the existing graph on the right and creates a line chart within the existing graph.
 *
 * @author artemc
 */

public class AddSecondChart {

	private final JFreeChart chart;
	private final CategoryPlot plot;
	private final DefaultCategoryDataset dataset;
	private final String[] categories;
	private final String yAxisLabel;
	private final Integer lowerLimit;
	private final Integer upperLimit;

	public AddSecondChart(final JFreeChart jFreeChart, final String yAxisLabel, final String[] categories, final Integer lowerLimit, final Integer upperLimit) {

		this.chart = jFreeChart;
		this.plot = chart.getCategoryPlot();
		this.categories = categories;
		this.dataset = new DefaultCategoryDataset();
		this.yAxisLabel = yAxisLabel;
		this.upperLimit = upperLimit;
		this.lowerLimit = lowerLimit;

	}

	public void addChartAndAxis(){

		//this.plot.setBackgroundPaint(new Color(0xEE, 0xEE, 0xFF));	    
		plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);

		//final CategoryAxis domainAxis = plot.getDomainAxis();
		//domainAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);

		final ValueAxis axis2 = new NumberAxis(yAxisLabel);
		axis2.setRange(lowerLimit, upperLimit);
		plot.setRangeAxis(1, axis2);
		plot.setDataset(1, dataset);
		plot.mapDatasetToRangeAxis(1, 1);


		final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
		renderer.setSeriesPaint(0, Color.BLACK);
		renderer.setSeriesStroke(0, new BasicStroke(4.0f));
		plot.setRenderer(1, renderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		plot.setDomainGridlinesVisible(true);

	}

	public void addChart(){


		plot.setDataset(1, dataset);
		plot.mapDatasetToRangeAxis(1, 0);


		final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
		renderer.setSeriesPaint(0, Color.BLACK);
		renderer.setSeriesStroke(0, new BasicStroke(4.0f));
		plot.setRenderer(1, renderer);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		plot.setDomainGridlinesVisible(true);

	}


	/**
	 * Adds a new data series to the chart with the specified title.
	 *
	 * @param title
	 * @param values
	 */
	public void addSeries(final String title, final Double[] values) {
		int cnt = 1;
		for (double value : values){
			String category = (cnt > this.categories.length ? Integer.toString(cnt) : this.categories[cnt-1]);
			this.dataset.addValue(value, title, category);
			cnt++;
		}
	}

}
