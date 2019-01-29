/* *********************************************************************** *
 * project: org.matsim.*
 * CountsGEHCurveGraph.java
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

package org.matsim.counts.algorithms.graphs;

import java.awt.Color;
import java.awt.Paint;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.counts.CountSimComparison;

public final class CountsGEHCurveGraph extends CountsGraph {

	private final DefaultCategoryDataset dataset;
	private String linkId;


	public CountsGEHCurveGraph(final List<CountSimComparison> ccl, final int iteration, final String filename){
		super(ccl, iteration, filename, filename);
		this.dataset = new DefaultCategoryDataset();
	}

	public void clearDatasets() {
		this.dataset.clear();
	}

	public void add2LoadCurveDataSets(final CountSimComparison cc ) {

		String h = Integer.toString(cc.getHour());
		this.dataset.addValue(cc.calculateGEHValue(), "GEH", h);
	}

	@Override
	public JFreeChart createChart(final int nbr) {
		String title = this.getChartTitle() + ", Iteration: " + this.iteration_;
		this.chart_ = ChartFactory.createBarChart(title, "Hour", "GEH", this.dataset,
				PlotOrientation.VERTICAL, 
				false, // legend?
				false, // tooltips?
				false // URLs?
				);
		CategoryPlot plot = this.chart_.getCategoryPlot();
//		plot.getRangeAxis().setRange(0.0, 10000.0); // do not set a fixed range for the single link graphs
		plot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_45);

		// BarRenderer renderer=(BarRenderer) plot.getRenderer();
//		BarRenderer renderer = new BarRenderer();
		/*
		 * Chooses the color adaptive based on the value to be shown by a bar.
		 * Values <= 5.0 are green, values >= 10 are red. In between the color is interpolated. 
		 */
		BarRenderer renderer = new BarRenderer() {
			@Override
			public Paint getItemPaint(final int row, final int column) {
				double value = dataset.getValue(row, column).doubleValue();
				
				if (value <= 5.0) return Color.green;
				else if (value >= 10) return Color.red;
				else {
					if (value < 7.5) {
						int mixed = mix(Color.yellow.getRGB(), Color.green.getRGB(), (7.5 - value) / 2.5);
						return new Color(mixed);
					} else {
						int mixed = mix(Color.red.getRGB(), Color.yellow.getRGB(), (10.0 - value) / 2.5);
						return new Color(mixed);
					}
				}
				
//				if (value <= 5.0) return Color.green;
//				else if (value >= 10) return Color.red;
//				else {
//					int mixed = mix(Color.red.getRGB(), Color.green.getRGB(), (10.0 - value) / 5.0);
//					return new Color(mixed);
//				}
			}
		};
		renderer.setSeriesOutlinePaint(0, Color.black);
		renderer.setSeriesPaint(0, Color.getHSBColor((float) 0.62, (float) 0.56, (float) 0.93));
		renderer.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());
		renderer.setItemMargin(0.0);
		
		// configure plot with light colors instead of the default 3D
		renderer.setShadowVisible(false);  
		renderer.setBarPainter(new StandardBarPainter()); 
		this.chart_.setBackgroundPaint(Color.getHSBColor((float) 0.0, (float) 0.0, (float) 0.93));
		plot.setBackgroundPaint(Color.white); 	     
		plot.setRangeGridlinePaint(Color.gray);		
		plot.setRangeGridlinesVisible(true);      

		plot.setRenderer(0, renderer);
		plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
		plot.mapDatasetToRangeAxis(1, 1);

		final CategoryAxis axis1 = plot.getDomainAxis();
		axis1.setCategoryMargin(0.25); // leave a gap of 25% between categories
		
		return this.chart_;
	}

	/**
	 * @return the linkId
	 */
	public String getLinkId() {
		return this.linkId;
	}

	/**
	 * @param linkId the linkId to set
	 */
	public void setLinkId(final String linkId) {
		this.linkId = linkId;
	}
	
	private static int mix(final int argb1, final int argb2, final double percentage) {
		int a1 = (argb1 >> 24) & 0xFF;
		int r1 = (argb1 >> 16) & 0xFF;
		int g1 = (argb1 >> 8) & 0xFF;
		int b1 = argb1 & 0xFF;

		int a2 = (argb2 >> 24) & 0xFF;
		int r2 = (argb2 >> 16) & 0xFF;
		int g2 = (argb2 >> 8) & 0xFF;
		int b2 = argb2 & 0xFF;

		double p = percentage;
		int a3 = (int) (a1 + p*(a2 - a1));
		int r3 = (int) (r1 + p*(r2 - r1));
		int g3 = (int) (g1 + p*(g2 - g1));
		int b3 = (int) (b1 + p*(b2 - b1));
		
		return (a3 << 24) | (r3 << 16) | (g3 << 8) | b3;
	}
}