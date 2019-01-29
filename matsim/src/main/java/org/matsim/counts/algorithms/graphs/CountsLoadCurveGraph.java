/* *********************************************************************** *
 * project: org.matsim.*
 * CountsLoadCurveGraph.java
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

/* 
 * Temporarily (?) removing relative error marked with "TRRE"
 * anhorni: 02.04.2008
 */

package org.matsim.counts.algorithms.graphs;

//TRRE: import java.awt.BasicStroke;
import java.awt.Color;
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
//TRRE:  import java.awt.geom.Rectangle2D;
//TRRE: import org.jfree.chart.axis.NumberAxis;
//TRRE: import org.jfree.chart.axis.ValueAxis;
//TRRE: import org.jfree.chart.plot.DatasetRenderingOrder;
//TRRE: import org.jfree.chart.renderer.category.LineAndShapeRenderer;



public final class CountsLoadCurveGraph extends CountsGraph {

	private final DefaultCategoryDataset dataset0;
	// TRRE: private final DefaultCategoryDataset dataset1;
	private String linkId;


	public CountsLoadCurveGraph(final List<CountSimComparison> ccl, final int iteration, final String filename){
		super(ccl, iteration, filename, filename);
		this.dataset0 = new DefaultCategoryDataset();
		// TRRE:  this.dataset1 = new DefaultCategoryDataset();
	}

	public void clearDatasets(){
		this.dataset0.clear();
		// TRRE: this.dataset1.clear();
	}

	public void add2LoadCurveDataSets(final CountSimComparison cc ) {
		String matsim_series = "Sim Volumes";
		String real_series = "Count Volumes";

		String h=Integer.toString(cc.getHour());

		this.dataset0.addValue(cc.getSimulationValue(),matsim_series, h);
		this.dataset0.addValue(cc.getCountValue(),real_series,h);

		//relative error
		// TRRE: this.dataset1.addValue(cc.calculateRelativeError(),"Signed Rel. Error",Integer.toString(cc.getHour()));
	}//add2LoadCurveDataSets


	@Override
	public JFreeChart createChart(final int nbr) {
		String title = this.getChartTitle() + ", Iteration: " + this.iteration_;
		this.chart_ = ChartFactory.createBarChart(title, "Hour", "Volumes [veh/h]", this.dataset0,
				PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
		CategoryPlot plot = this.chart_.getCategoryPlot();
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
		
		// configure plot with light colors instead of the default 3D
		renderer.setShadowVisible(false);  
		renderer.setBarPainter( new StandardBarPainter() ); 
		this.chart_.setBackgroundPaint(Color.getHSBColor((float) 0.0, (float) 0.0, (float) 0.93));
		plot.setBackgroundPaint(Color.white); 	     
		plot.setRangeGridlinePaint(Color.gray);		
		plot.setRangeGridlinesVisible(true);      

		plot.setRenderer(0, renderer);
		plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
		// TRRE:  plot.setDataset(1, this.dataset1);
		plot.mapDatasetToRangeAxis(1, 1);

		final CategoryAxis axis1 = plot.getDomainAxis();
		axis1.setCategoryMargin(0.25); // leave a gap of 25% between categories

		/*  TRRE: 
		final ValueAxis axis2 = new NumberAxis("Signed Rel. Error [%]");
		plot.setRangeAxis(1, axis2);

		final LineAndShapeRenderer renderer2 = new LineAndShapeRenderer();
		renderer2.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());
		renderer2.setSeriesShape(0, new Rectangle2D.Double(-1.5, -1.5, 3.0, 3.0));
		renderer2.setSeriesPaint(0, Color.black);
		renderer2.setBaseStroke(new BasicStroke(2.5f));
		plot.setRenderer(1, renderer2);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		*/
		
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
}
