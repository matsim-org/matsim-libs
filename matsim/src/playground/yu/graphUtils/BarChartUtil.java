/* *********************************************************************** *
 * project: org.matsim.*
 * BarChartUtil.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.graphUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author yu
 * 
 */
public class BarChartUtil extends ChartUtil {
	public BarChartUtil(String title, String categoryAxisLabel,
			String valueAxisLabel) {
		super(title, categoryAxisLabel, valueAxisLabel);
	}

	protected JFreeChart createChart(String title, String xAxisLabel,
			String yAxisLabel) {
		chart_ = ChartFactory.createBarChart(title, xAxisLabel, yAxisLabel,
				dataset0, PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
		return chart_;
	}
	
	public void setDataSets(DefaultCategoryDataset[] dataSets) {
		super.setDataSets(dataSets);
//		plot_.getDomainAxis().setCategoryLabelPositions(
//				CategoryLabelPositions.UP_45);
//		// render
//		BarRenderer renderer = new BarRenderer();
//		renderer.setSeriesOutlinePaint(0, Color.black);
//		renderer.setSeriesOutlinePaint(1, Color.black);
//		renderer.setSeriesPaint(0, Color.getHSBColor((float) 0.62,
//				(float) 0.56, (float) 0.93));
//		// Color.orange gives a dirty yellow!
//		renderer.setSeriesPaint(1, Color.getHSBColor((float) 0.1, (float) 0.79,
//				(float) 0.89));
//		renderer.setSeriesToolTipGenerator(0,
//				new StandardCategoryToolTipGenerator());
//		renderer.setSeriesToolTipGenerator(1,
//				new StandardCategoryToolTipGenerator());
//		renderer.setItemMargin(0.0);
//		plot_.setRenderer(0, renderer);
//		plot_.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
//		plot_.mapDatasetToRangeAxis(1, 1);
//		final CategoryAxis axis1 = plot_.getDomainAxis();
//		axis1.setCategoryMargin(0.25); // leave a gap of 25% between categories
//
//		final ValueAxis axis2 = new NumberAxis("Signed Rel. Error [%]");// ???????ChartFactory.createBarChart...
//		plot_.setRangeAxis(1, axis2);
//		// render2
//		final LineAndShapeRenderer renderer2 = new LineAndShapeRenderer();
//		renderer2.setSeriesToolTipGenerator(0,
//				new StandardCategoryToolTipGenerator());
//		renderer2.setSeriesShape(0,
//				new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));
//		renderer2.setSeriesPaint(0, Color.black);
//		renderer2.setBaseStroke(new BasicStroke(2.5f));
//		plot_.setRenderer(1, renderer2);
//		plot_.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
	}
}
