/* *********************************************************************** *
 * project: org.matsim.*
 * DgChartTemplate
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.charts;

import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * Simple example for a custom JFreeChart 
 * 
 * @author dgrether
 *
 */
public class DgChartTemplate {
  
  private XYSeriesCollection dataset;

  
  private void createDataSet() {
    
    double xvalue = 0;
    double yvalue = 0;
    
    this.dataset = new XYSeriesCollection();
    XYSeries series = new XYSeries("Series name", false, true);
    this.dataset.addSeries(series);
    series.add(xvalue, yvalue);
  }

  
  public JFreeChart createChart() {
    XYPlot plot = new XYPlot();
    plot.setDataset(0, this.getDataset());
    JFreeChart chart = new JFreeChart("", plot);
    chart.setBackgroundPaint(ChartColor.WHITE);
    chart.setTextAntiAlias(true);
//    chart.removeLegend();
    return chart;
  }
  
  public XYSeriesCollection getDataset() {
    if (this.dataset == null) {
      createDataSet();
    }
    return dataset;
  }
}
