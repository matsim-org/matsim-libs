/* *********************************************************************** *
 * project: org.matsim.*
 * DgGreenSplitPerIterationGraph
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
package playground.dgrether.signalsystems;

import java.awt.BasicStroke;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.signalsystems.control.SignalGroupState;

import playground.dgrether.analysis.charts.DgAxisBuilder;
import playground.dgrether.analysis.charts.DgDefaultAxisBuilder;
import playground.dgrether.analysis.charts.utils.DgColorScheme;


/**
 * @author dgrether
 *
 */
public class DgGreenSplitPerIterationGraph {
  private XYSeriesCollection dataset;
  
  private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();

  private ControlerConfigGroup controllerConfig;

  private Id signalSystemId;

  private SortedMap<Integer, DgSignalSystemData> iterationSystemDataMap;
  
  public DgGreenSplitPerIterationGraph(ControlerConfigGroup controlerConfigGroup, Id signalSystemId){
    this.controllerConfig =  controlerConfigGroup;
    this.signalSystemId = signalSystemId;
    this.iterationSystemDataMap = new TreeMap<Integer, DgSignalSystemData>();
  }
  
  public void addIterationData(DgSignalGreenSplitHandler h, Integer iteration) {
    DgSignalSystemData systemData = h.getSystemIdDataMap().get(this.signalSystemId);
    if (systemData == null) {
      throw new RuntimeException("null in iteration " + iteration);
    }
    this.iterationSystemDataMap.put(iteration, systemData);
  }
  
  private void createDataSet() {
    this.dataset = new XYSeriesCollection();
    Map<Id, XYSeries> signalGroupGreenXYSeries = new HashMap<Id, XYSeries>();
//    Map<Id, XYSeries> signalGroupRedXYSeries = new HashMap<Id, XYSeries>();
    for (Integer it : this.iterationSystemDataMap.keySet()) {
      DgSignalSystemData sd = this.iterationSystemDataMap.get(it);
      for (Id groupId : sd.getSystemGroupDataMap().keySet()) {
        XYSeries series = signalGroupGreenXYSeries.get(groupId);
        if (series == null) {
          series = new XYSeries("group id green" + groupId, false, true);
          signalGroupGreenXYSeries.put(groupId, series);
          this.dataset.addSeries(series);
        }
        Map<SignalGroupState, Double> stateTimeMap = sd.getSystemGroupDataMap().get(groupId).getStateTimeMap();
        Double green = stateTimeMap.get(SignalGroupState.GREEN);
        Double red = stateTimeMap.get(SignalGroupState.RED);
        Double greenSplit = null;
        if (green == null) {
          greenSplit = 0.0;
        }
        if (red == null) {
          greenSplit = 100.0;
        }
        if (greenSplit == null){
          greenSplit = green / (red + green) * 100.0;
        }
        series.add(it, greenSplit);
        
//        XYSeries redSeries = signalGroupRedXYSeries.get(groupId);
//        if (redSeries == null) {
//          redSeries = new XYSeries("group id red" + groupId, false, true);
//          signalGroupRedXYSeries.put(groupId, redSeries);
//          this.dataset.addSeries(redSeries);
//        }
//        redSeries.add(it, sd.getSystemGroupDataMap().get(groupId).getStateTimeMap().get(SignalGroupState.RED));

        
      }
    }
  }

  
  public JFreeChart createChart() {
    XYPlot plot = new XYPlot();
    ValueAxis xAxis = this.axisBuilder.createValueAxis("Iteration");
    xAxis.setRange(this.controllerConfig.getFirstIteration(), this.controllerConfig.getLastIteration() + 2);
    ValueAxis yAxis = this.axisBuilder.createValueAxis("GreenTime");
//    yAxis.setRange(-0.05, 0.3);
//    xAxis.setVisible(false);
//    xAxis.setFixedAutoRange(1.0);
    plot.setDomainAxis(xAxis);
    plot.setRangeAxis(yAxis);
    
    DgColorScheme colorScheme = new DgColorScheme();
    
    XYItemRenderer renderer2;
    renderer2 = new XYLineAndShapeRenderer(true, true);
    renderer2.setSeriesItemLabelsVisible(0, true);
//    renderer2.setSeriesItemLabelGenerator(0, this.labelGenerator);
    plot.setDataset(0, this.getDataset());
    renderer2.setSeriesStroke(0, new BasicStroke(2.0f));
    renderer2.setSeriesOutlineStroke(0, new BasicStroke(3.0f));
    renderer2.setSeriesPaint(0, colorScheme.getColor(1, "a"));
    renderer2.setSeriesStroke(1, new BasicStroke(2.0f));
    renderer2.setSeriesOutlineStroke(1, new BasicStroke(3.0f));
    renderer2.setSeriesPaint(1, colorScheme.getColor(2, "a"));
    renderer2.setSeriesStroke(2, new BasicStroke(2.0f));
    renderer2.setSeriesOutlineStroke(2, new BasicStroke(3.0f));
    renderer2.setSeriesPaint(2, colorScheme.getColor(3, "a"));
    renderer2.setSeriesStroke(3, new BasicStroke(2.0f));
    renderer2.setSeriesOutlineStroke(3, new BasicStroke(3.0f));
    renderer2.setSeriesPaint(3, colorScheme.getColor(4, "a"));
    
    plot.setRenderer(0, renderer2);
    
    JFreeChart chart = new JFreeChart("", plot);
    chart.setBackgroundPaint(ChartColor.WHITE);
    chart.getLegend().setItemFont(this.axisBuilder.getAxisFont());
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
