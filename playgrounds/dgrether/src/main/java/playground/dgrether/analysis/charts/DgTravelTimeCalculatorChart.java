/* *********************************************************************** *
 * project: org.matsim.*
 * DgTravelTimeCalulatorChart
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

import java.awt.BasicStroke;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.dgrether.analysis.charts.interfaces.DgChart;
import playground.dgrether.analysis.charts.utils.DgColorScheme;


/**
 * @author dgrether
 *
 */
public class DgTravelTimeCalculatorChart implements DgChart {

  private static final Logger log = Logger
      .getLogger(DgTravelTimeCalculatorChart.class);
  
  private TravelTimeCalculator calculator;

  private Map<List<Id>, XYSeries> linkIds = new HashMap<List<Id>, XYSeries>();

  private double endTime = Double.POSITIVE_INFINITY;

  private double startTime = 0;
  
  
  public DgTravelTimeCalculatorChart(TravelTimeCalculator travelTimeCalculator) {
    this.calculator = travelTimeCalculator;
  }

  public void addLinkId(List<Id> ids) {
    StringBuilder title = new StringBuilder();
    title.append("Link");
    for (Id id : ids) {
      title.append(" ");
      title.append(id);
    }
    this.linkIds.put(ids, new XYSeries(title.toString(), false, false));
  }
  
  private XYSeriesCollection createDataSet(){
    XYSeriesCollection dataset = new XYSeriesCollection();
    
    int numSlots = this.calculator.getNumSlots();
    int binSize = this.calculator.getTimeSlice();
    int maxtime = numSlots * binSize;
    
    double startSecond = startTime;
    double endSecond;
    if (maxtime < endTime) {
      endSecond = maxtime;
    }
    else {
      endSecond = endTime;
    }
    
    
    double tt;
    XYSeries series;
    for (Entry<List<Id>, XYSeries> e : this.linkIds.entrySet()){
      series = e.getValue();
      dataset.addSeries(series);
//      log.error("link: "+ e.getKey());
      for (double i = startSecond; i < endSecond; i++) {
        tt = 0;
        for (Id id : e.getKey()){
          tt += this.calculator.getLinkTravelTime(id, i);
        }
//        log.error("time: " + i + " tt " + tt);
        e.getValue().add(i, tt);
      }
    }
    
    return dataset;
  }
  

  public JFreeChart createChart() {
    XYSeriesCollection dataset = this.createDataSet();
    XYPlot plot = new XYPlot();
    DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();
    ValueAxis xAxis = axisBuilder.createValueAxis("Simulation Time");
//    xAxis.setRange(this.controllerConfig.getFirstIteration(), this.controllerConfig.getLastIteration() + 2);
    ValueAxis yAxis = axisBuilder.createValueAxis("Travel Time");
//    yAxis.setRange(-0.05, 0.3);
//    xAxis.setVisible(false);
//    xAxis.setFixedAutoRange(1.0);
    plot.setDomainAxis(xAxis);
    plot.setRangeAxis(yAxis);
    
    DgColorScheme colorScheme = new DgColorScheme();
    
    XYItemRenderer renderer2;
    renderer2 = new XYLineAndShapeRenderer(true, false);
    renderer2.setSeriesItemLabelsVisible(0, true);
//    renderer2.setSeriesItemLabelGenerator(0, this.labelGenerator);
    plot.setDataset(0, dataset);
    renderer2.setSeriesStroke(0, new BasicStroke(1.0f));
    renderer2.setSeriesOutlineStroke(0, new BasicStroke(1.0f));
    renderer2.setSeriesPaint(0, colorScheme.getColor(1, "a"));
    renderer2.setSeriesStroke(1, new BasicStroke(1.0f));
    renderer2.setSeriesOutlineStroke(1, new BasicStroke(1.0f));
    renderer2.setSeriesPaint(1, colorScheme.getColor(2, "a"));
    renderer2.setSeriesStroke(2, new BasicStroke(1.0f));
    renderer2.setSeriesOutlineStroke(2, new BasicStroke(1.0f));
    renderer2.setSeriesPaint(2, colorScheme.getColor(3, "a"));
    renderer2.setSeriesStroke(3, new BasicStroke(1.0f));
    renderer2.setSeriesOutlineStroke(3, new BasicStroke(1.0f));
    renderer2.setSeriesPaint(3, colorScheme.getColor(4, "a"));
    
    plot.setRenderer(0, renderer2);
    
    JFreeChart chart = new JFreeChart("", plot);
    chart.setBackgroundPaint(ChartColor.WHITE);
    chart.getLegend().setItemFont(axisBuilder.getAxisFont());
    chart.setTextAntiAlias(true);
//    chart.removeLegend();
    return chart;
  }

  public void setStartTime(double sec) {
    this.startTime  = sec;
  }

  public void setEndTime(double sec) {
    this.endTime = sec;
  }

  
  
}
