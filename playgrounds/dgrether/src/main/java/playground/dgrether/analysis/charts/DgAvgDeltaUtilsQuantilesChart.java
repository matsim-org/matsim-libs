/* *********************************************************************** *
 * project: org.matsim.*
 * DgAvgDeltaUtilsQuantilesChart
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
package playground.dgrether.analysis.charts;

import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.List;

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
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.analysis.charts.utils.DgColorScheme;
import playground.dgrether.analysis.charts.utils.DgXYLabelGenerator;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonDataIncomeComparator;


/**
 * @author dgrether
 *
 */
public class DgAvgDeltaUtilsQuantilesChart {
	
	private static final Logger log = Logger.getLogger(DgAvgDeltaUtilsQuantilesChart.class);
	
	protected DgAnalysisPopulation ana;
	
	protected int nQuantiles = 10;
	
	private XYSeriesCollection dataset;
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();

	private DgXYLabelGenerator labelGenerator;
	
	public DgAvgDeltaUtilsQuantilesChart(DgAnalysisPopulation ana, Id runId1, Id runId2) {
		this.ana = ana;
		this.labelGenerator = new DgXYLabelGenerator();
		this.dataset = this.createDatasets(runId1, runId2);
	}
	
	private Tuple<XYSeries,List<String>> createXYSeries(String title, DgAnalysisPopulation pop, Id runId1, Id runId2) {
		List<DgAnalysisPopulation> quantiles = pop.getQuantiles(this.nQuantiles, new DgPersonDataIncomeComparator());
		XYSeries series = new XYSeries(title, false, true);
		List<String> labels = new ArrayList<String>();
		double income= 0.0;
		double i = -0.5;
		for (DgAnalysisPopulation p : quantiles){
			i++;
			Double avgScore = p.calcAverageScoreDifference(runId1, runId2);
			p.calculateIncomeData();
			income += p.getTotalIncome();
			double incomeLocation = 100.0 * i/this.nQuantiles;
			series.add(incomeLocation, avgScore);
			labels.add("Groupsize: " + p.getPersonData().size());
		}
		return new Tuple<XYSeries, List<String>>(series, labels);
	}
	

	
	private XYSeriesCollection createDatasets(Id runId1, Id runId2) {
		XYSeriesCollection ds = new XYSeriesCollection();
		Tuple<XYSeries, List<String>> seriesLabels = this.createXYSeries("Mean "+  '\u0394' + "Utility", this.ana, runId1, runId2);
		ds.addSeries(seriesLabels.getFirst());
		this.labelGenerator.setLabels(0, seriesLabels.getSecond());
		return ds;
	}
	
	public JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		ValueAxis xAxis = this.axisBuilder.createValueAxis("% of Population Sorted by Income");
		xAxis.setRange(0.0, 102.0);
		ValueAxis yAxis = this.axisBuilder.createValueAxis("Delta Utils [Utils]");
		yAxis.setRange(-0.05, 0.3);
//		xAxis.setVisible(false);
//		xAxis.setFixedAutoRange(1.0);
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(yAxis);
		
		DgColorScheme colorScheme = new DgColorScheme();
		
		XYItemRenderer renderer2;
		renderer2 = new XYLineAndShapeRenderer(true, true);
		renderer2.setSeriesItemLabelsVisible(0, true);
//		renderer2.setSeriesItemLabelGenerator(0, this.labelGenerator);
		plot.setDataset(0, this.dataset);
		renderer2.setSeriesStroke(0, new BasicStroke(2.0f));
		renderer2.setSeriesOutlineStroke(0, new BasicStroke(3.0f));
		renderer2.setSeriesPaint(0, colorScheme.getColor(1, "a"));
		plot.setRenderer(0, renderer2);
		
		JFreeChart chart = new JFreeChart("", plot);
		chart.setBackgroundPaint(ChartColor.WHITE);
		chart.getLegend().setItemFont(this.axisBuilder.getAxisFont());
		chart.setTextAntiAlias(true);
		chart.removeLegend();
		return chart;
	}
	
	public XYSeriesCollection getDataset() {
		return dataset;
	}
	
	public int getNumberOfClasses() {
		return nQuantiles;
	}

	public void setNumberOfClasses(int numberOfClasses) {
		this.nQuantiles = numberOfClasses;
	}

}
