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

import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author droeder
 *
 */
public class DaCharts {
	  private XYSeriesCollection dataset;

	  
	  private XYSeriesCollection createDataSet() {
	    
	    double xvalue = 0;
	    double yvalue = 0;
	    
	    this.dataset = new XYSeriesCollection();
	    XYSeries series = new XYSeries("Series name", false, true);
	    this.dataset.addSeries(series);
	    series.add(xvalue, yvalue);
	    
	    return dataset;
	  }

	  
	  public JFreeChart createChart() {
	    XYPlot plot = new XYPlot();
	    plot.setDataset(0, this.getDataset());
	    JFreeChart chart = new JFreeChart("", plot);
	    chart.setBackgroundPaint(ChartColor.WHITE);
	    chart.setTextAntiAlias(true);
//	    chart.removeLegend();
	    
	    return chart;
	  }
	  
	  public XYSeriesCollection getDataset() {
	    if (this.dataset == null) {
	      createDataSet();
	    }
	    return dataset;
	  }

}
