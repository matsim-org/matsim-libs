/* *********************************************************************** *
 * project: org.matsim.*
 * DgCountPerIterationGraph
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.linkanalysis;

import java.awt.BasicStroke;

import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.config.groups.ControlerConfigGroup;

import playground.dgrether.analysis.charts.DgAxisBuilder;
import playground.dgrether.analysis.charts.DgDefaultAxisBuilder;
import playground.dgrether.analysis.charts.utils.DgColorScheme;


/**
 * @author dgrether
 *
 */
public class DgCountPerIterationGraph {
	
	private XYSeriesCollection dataset;
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();

  private ControlerConfigGroup controllerConfig;


	
	public DgCountPerIterationGraph(ControlerConfigGroup controlerConfigGroup){
		this.dataset = new XYSeriesCollection();
		this.controllerConfig =  controlerConfigGroup;
	}
	
	public void addCountEventHandler(TTInOutflowEventHandler h) {
		this.addCountEventHandler(h, "count on link " + h.getLinkId());
	}
	
	public void addCountEventHandler(TTInOutflowEventHandler h, String seriesTitle){
		XYSeries series = new XYSeries(seriesTitle, false, true);
		for (Integer i : h.getCountPerIteration().keySet()){
			series.add(i, h.getCountPerIteration().get(i));
		}
		this.dataset.addSeries(series);
	}

	
	public JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		ValueAxis xAxis = this.axisBuilder.createValueAxis("Iteration");
		xAxis.setRange(this.controllerConfig.getFirstIteration(), this.controllerConfig.getLastIteration() + 2);
		ValueAxis yAxis = this.axisBuilder.createValueAxis("Trips");
//		yAxis.setRange(-0.05, 0.3);
//		xAxis.setVisible(false);
//		xAxis.setFixedAutoRange(1.0);
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(yAxis);
		
		plot.setDataset(0, this.dataset);

		DgColorScheme colorScheme = new DgColorScheme();
		XYItemRenderer renderer2;
		renderer2 = new XYLineAndShapeRenderer(true, true);
		for (int i = 0; i < this.dataset.getSeriesCount(); i++){
			renderer2.setSeriesItemLabelsVisible(i, true);
			renderer2.setSeriesOutlineStroke(i, new BasicStroke(3.0f));
			renderer2.setSeriesStroke(i, new BasicStroke(2.0f));
			renderer2.setSeriesPaint(i, colorScheme.getColor(i+1, "a"));
		}
//		renderer2.setSeriesItemLabelGenerator(0, this.labelGenerator);
//		renderer2.setSeriesStroke(1, new BasicStroke(2.0f));
//		renderer2.setSeriesOutlineStroke(1, new BasicStroke(3.0f));
//		renderer2.setSeriesPaint(1, colorScheme.getColor(2, "a"));
		
		plot.setRenderer(0, renderer2);
		
		JFreeChart chart = new JFreeChart("", plot);
		chart.setBackgroundPaint(ChartColor.WHITE);
		chart.getLegend().setItemFont(this.axisBuilder.getAxisFont());
		chart.setTextAntiAlias(true);
//		chart.removeLegend();
		return chart;
	}
	
	public XYSeriesCollection getDataset() {
		return dataset;
	}


}
