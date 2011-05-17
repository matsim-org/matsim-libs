/* *********************************************************************** *
 * project: org.matsim.*
 * DaSignalPlanChart
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems.utils;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.signalsystems.model.SignalGroupState;

import playground.dgrether.analysis.charts.DgDefaultAxisBuilder;

/**
 * @author dgrether
 * 
 */
public class DgSignalPlanChart {

	
	private static final Logger log = Logger.getLogger(DgSignalPlanChart.class);
	
	private DefaultCategoryDataset dataset;
	private JFreeChart jChart;
	private Map<Integer, Color> seriesColor = new HashMap<Integer, Color>();
	private double tMin;
	private double tMax;

	public DgSignalPlanChart(double tMinSec, double tMaxSec) {
		this.tMin = tMinSec;
		this.tMax = tMaxSec;
	}

	public void addData(SortedSet<SignalGroupStateChangedEvent> systemEvents) {
		this.dataset = new DefaultCategoryDataset();
		Integer i = 0;
		Map<Id, SignalGroupStateChangedEvent> id2lastStateChangeEventMap = new HashMap<Id, SignalGroupStateChangedEvent>();
		for (SignalGroupStateChangedEvent e : systemEvents) {
			if (e.getTime() < this.tMin){
				continue;
			}
			else if (e.getTime() > this.tMax){
				break;
			}
			else {
				SignalGroupStateChangedEvent lastStateChange = id2lastStateChangeEventMap.get(e.getSignalGroupId());
				if (lastStateChange == null){
					//process initial state
					this.dataset.addValue(e.getTime(), i, e.getSignalGroupId());
					log.info("added value: " + e.getTime() + " group id " + e.getSignalGroupId() + " row key: " + i);
					setSeriesColor(i, null);
				}
				else {
					//process state
					double duration = e.getTime() - lastStateChange.getTime();
					this.dataset.addValue(duration, i, lastStateChange.getSignalGroupId());
					log.info("added value: " + duration + " group id " + lastStateChange.getSignalGroupId() + " row key: " + i);
					this.setSeriesColor(i, lastStateChange.getNewState());
				}
				i++;
				id2lastStateChangeEventMap.put(e.getSignalGroupId(), e);
			}
		}
		
	}

	private void addData(Map<Id, TreeMap<Double, SignalGroupState>> id2Time2StateMap) {
		Integer i = 0;
		for (Entry<Id, TreeMap<Double, SignalGroupState>> id2mapEntry : id2Time2StateMap.entrySet()) {
			double lastTime = 0.0;
			SignalGroupState tempState = SignalGroupState.RED;
			for (Entry<Double, SignalGroupState> doubleStateEntry : id2mapEntry.getValue().entrySet()) {
				double time = doubleStateEntry.getKey();
				if (time >= tMin && time <= tMax) {
					this.dataset.addValue(time - lastTime, i, id2mapEntry.getKey());
					setSeriesColor(i, tempState);
					for (Entry<Id, TreeMap<Double, SignalGroupState>> eee : id2Time2StateMap.entrySet()) {
						if (!eee.getKey().equals(id2mapEntry.getKey())) {
							this.dataset.addValue(0, i, eee.getKey());
						}
					}
				}
				else if (time > tMax) {
					this.dataset.addValue(time - lastTime, i, id2mapEntry.getKey());
					setSeriesColor(i, tempState);
					for (Entry<Id, TreeMap<Double, SignalGroupState>> eee : id2Time2StateMap.entrySet()) {
						if (!eee.getKey().equals(id2mapEntry.getKey())) {
							this.dataset.addValue(0, i, eee.getKey());
						}
					}
					i++;
					break;
				}
				i++;
				lastTime = time;
				tempState = doubleStateEntry.getValue();
			}
			
		}
	}

	private void setSeriesColor(Integer i, SignalGroupState state) {
		if (state == null){
			seriesColor.put(i, new Color(200, 200, 200, 255));
		}
		else if (state.equals(SignalGroupState.RED)) {
			seriesColor.put(i, new Color(163, 0, 0, 255));
		}
		else if (state.equals(SignalGroupState.GREEN)) {
			seriesColor.put(i, new Color(0, 102, 0, 255));
		}
		else if (state.equals(SignalGroupState.YELLOW)) {
			seriesColor.put(i, new Color(255, 204, 0, 255));
		}
		else if (state.equals(SignalGroupState.REDYELLOW)) {
			seriesColor.put(i, new Color(255, 102, 0, 255));
		}
	}

	public JFreeChart createSignalPlanChart(String title, String xAxisTitle, String yAxisTitle) {
		JFreeChart chart = ChartFactory.createStackedBarChart(title, xAxisTitle, yAxisTitle, this.dataset,
				PlotOrientation.HORIZONTAL, false, false, false);
		DgDefaultAxisBuilder axis = new DgDefaultAxisBuilder();
		CategoryPlot plot = chart.getCategoryPlot();
		
		plot.setBackgroundPaint(Color.white);
		plot.setDomainGridlinePaint(Color.lightGray);
		plot.setRangeGridlinePaint(Color.black);
		plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
		CategoryAxis xAxis = axis.createCategoryAxis(xAxisTitle);
		plot.setDomainAxis(xAxis);
		ValueAxis yAxis = axis.createValueAxis(yAxisTitle);
		yAxis.setUpperBound(this.tMax);
		yAxis.setLowerBound(this.tMin);
		plot.setRangeAxis(yAxis);

		final BarRenderer renderer = (BarRenderer) plot.getRenderer();
		renderer.findRangeBounds(dataset);
		renderer.setShadowVisible(false);
//		renderer.setItemMargin(10.005);
		renderer.setMaximumBarWidth(0.2);
		for (Entry<Integer, Color> ee : seriesColor.entrySet()) {
			renderer.setSeriesPaint(ee.getKey(), ee.getValue());
//			renderer.setSeriesStroke(ee.getKey(), new BasicStroke(50));
		}
		plot.setRenderer(renderer);
//		chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
//		chart.setBackgroundPaint(ChartColor.WHITE);
//		chart.removeLegend();
		return chart;
	}

}
