/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.charts;

import java.awt.Color;
import java.awt.Paint;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.matsim.core.utils.io.IOUtils;

import playground.droeder.DaColorScheme;

/**
 * @author droeder
 *
 */
public class DaBarChart {
	  private DefaultCategoryDataset dataset;
	  private JFreeChart jChart;
	  
	  public DaBarChart(){
		  this.dataset = new DefaultCategoryDataset();
	  }
	  
	  public void addSeries(String category, Map<Number, Number> seriesAndX){
		  for (Entry<Number, Number> e : seriesAndX.entrySet()){
			  dataset.addValue(e.getValue(), category, String.valueOf(e.getKey()));
		  }
	  }

	  
	  public JFreeChart createChart(String title, String xAxis, String yAxis) {
		JFreeChart chart = ChartFactory.createBarChart(title, xAxis, yAxis, dataset, PlotOrientation.VERTICAL, true, false, false);
		DaAxisBuilder axis = new DaAxisBuilder();
		CategoryPlot plot = chart.getCategoryPlot();
		
	    plot.setBackgroundPaint(Color.white);
	    plot.setDomainGridlinePaint(Color.lightGray);
	    plot.setRangeGridlinePaint(Color.black);
	    plot.setDomainAxis(axis.createCategoryAxis(xAxis));
	    plot.setRangeAxis(axis.createValueAxis(yAxis));
		
		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
		
		renderer.findRangeBounds(dataset);
		DaColorScheme cs = new DaColorScheme();
		for(int i=1; i< 1+dataset.getRowCount(); i++){
			renderer.setSeriesPaint(i, cs.getColor(i));
		}
		return chart;

	  }
	  
}
