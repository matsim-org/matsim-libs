/* *********************************************************************** *
 * project: org.matsim.*
 * GraphUtils.java
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
package playground.yu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author ychen
 *
 */
public class GraphUtils {
	protected JFreeChart chart_;
	private DefaultCategoryDataset dataset0,dataset1;
	public GraphUtils(){
		dataset0 = new DefaultCategoryDataset();
		dataset1 = new DefaultCategoryDataset();
	}
	public JFreeChart createBarChart(String title, String categoryAxisLabel, String valueAxisLabel){
//		String title = this.getChartTitle() + ", Iteration: " + this.iteration_;
		chart_ = ChartFactory.createBarChart(title, categoryAxisLabel, valueAxisLabel, dataset0,
				PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
		CategoryPlot plot = chart_.getCategoryPlot();
//		plot.getRangeAxis().setRange(0.0, 10000.0); // do not set a fixed range for the single link graphs
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// BarRenderer renderer=(BarRenderer) plot.getRenderer();
		BarRenderer renderer = new BarRenderer();
		renderer.setSeriesOutlinePaint(0, Color.black);
		renderer.setSeriesOutlinePaint(1, Color.black);
		renderer.setSeriesPaint(0, Color.getHSBColor((float) 0.62, (float) 0.56, (float) 0.93));
		// Color.orange gives a dirty yellow!
		renderer.setSeriesPaint(1, Color.getHSBColor((float) 0.1, (float) 0.79, (float) 0.89));
		renderer.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());
		renderer.setSeriesToolTipGenerator(1, new StandardCategoryToolTipGenerator());
		renderer.setItemMargin(0.0);

		plot.setRenderer(0, renderer);
		plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
		plot.setDataset(1, this.dataset1);
		plot.mapDatasetToRangeAxis(1, 1);

		final CategoryAxis axis1 = plot.getDomainAxis();
		axis1.setCategoryMargin(0.25); // leave a gap of 25% between categories

		final ValueAxis axis2 = new NumberAxis("Signed Rel. Error [%]");
		plot.setRangeAxis(1, axis2);

		final LineAndShapeRenderer renderer2 = new LineAndShapeRenderer();
		renderer2.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());
		renderer2.setSeriesShape(0, new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));
		renderer2.setSeriesPaint(0, Color.black);
		renderer2.setBaseStroke(new BasicStroke(2.5f));
		plot.setRenderer(1, renderer2);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		return chart_;
	}
	public void addData(double[] values, Comparable[] args, Comparable[] rowKeys){
		
	}
	public void saveAsPng(String filename, int width, int height){
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), chart_, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
