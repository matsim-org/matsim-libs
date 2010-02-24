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
package playground.droeder;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author droeder
 *
 */
public class DaChartWriter {
	  private XYSeriesCollection datasetXY;
	  
	  private void createDataSetXY(){
		  this.datasetXY = new XYSeriesCollection();
	  }
	  
	  
	  public XYSeries createXySeries(String seriesName, Map<Number, Number> data){
		  XYSeries series = new XYSeries(seriesName, false, true);
		  for (Entry<Number, Number> e : data.entrySet()){
			  series.add(e.getKey(), e.getValue());
		  }
		  return series;
	  }

	  public void addXySeries(XYSeries series) {
		  if (this.datasetXY == null) {
		      createDataSetXY();
		    }
		  this.datasetXY.addSeries(series);
	  }

	  
	  public JFreeChart createChart(String title, String xAxis, String yAxis) {
//	    XYPlot plot = new XYPlot();
//	    plot.setDataset(0, this.getDataset());
//	    JFreeChart chart = new JFreeChart("", plot);
//	    chart.setBackgroundPaint(ChartColor.WHITE);
//	    chart.setTextAntiAlias(true);
////	    chart.removeLegend();
		JFreeChart chart = ChartFactory.createXYLineChart(title, xAxis, yAxis, this.datasetXY, PlotOrientation.HORIZONTAL , true, false, false);
		
		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.black);
		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.black);
		return chart;

	  }
	  
	  public XYSeriesCollection getDataset() {
	    if (this.datasetXY == null) {
	      createDataSetXY();
	    }
	    return this.datasetXY;
	  }
	  
		public static void writeChart(String filename, Integer width, Integer height, JFreeChart jchart){
			writeToPng(filename, width, height, jchart);
		}
		
		private static void writeToPng(String path, Integer width, Integer height, JFreeChart jchart) {
			String title = path + jchart.getTitle().toString() + ".png";
			
			try {
				ChartUtilities.saveChartAsPNG(new File(title), jchart, width, height, null, true, 9);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	  


}
