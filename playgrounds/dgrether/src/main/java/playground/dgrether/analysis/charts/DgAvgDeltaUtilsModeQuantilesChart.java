/* *********************************************************************** *
 * project: org.matsim.*
 * DgDeltaScoreIncomeChart
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
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.analysis.charts.interfaces.DgXYChart;
import playground.dgrether.analysis.charts.utils.DgColorScheme;
import playground.dgrether.analysis.charts.utils.DgXYLabelGenerator;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgModeSwitchPlanTypeAnalyzer;
import playground.dgrether.analysis.population.DgPersonDataIncomeComparator;
/**
 * 
 * @author dgrether
 */
public class DgAvgDeltaUtilsModeQuantilesChart implements DgXYChart {
	
	private static final Logger log = Logger.getLogger(DgAvgDeltaUtilsModeQuantilesChart.class);
	protected DgAnalysisPopulation ana;
	
	protected int nQuantiles = 10;
	
	protected XYSeriesCollection dataset;
	
	protected DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();

	private DgXYLabelGenerator labelGenerator;
	private int threshold;

	public DgAvgDeltaUtilsModeQuantilesChart(DgAnalysisPopulation ana, int threshold, Id runId1, Id runId2) {
		this.ana = ana;
		this.threshold = threshold;
		this.labelGenerator = new DgXYLabelGenerator();
		this.dataset = this.createDatasets(runId1, runId2);
	}
	
	private XYSeriesCollection createDatasets(Id runId1, Id runId2) {
		List<DgAnalysisPopulation> quantiles = this.ana.getQuantiles(this.nQuantiles, new DgPersonDataIncomeComparator());
		XYSeries car2carSeries = new XYSeries("Mean "+  '\u0394' + "Utility Car2Car", false, true);
		XYSeries pt2ptSeries = new XYSeries("Mean "+  '\u0394' + "Utility Pt2Pt", false, true);
		XYSeries pt2carSeries = new XYSeries("Mean "+  '\u0394' + "Utility Pt2Car", false, true);
		XYSeries car2ptSeries = new XYSeries(		"Mean "+  '\u0394' + "Utility Car2Pt", false, true);
		double quantile = -0.5;
		Double avgUtil = 0.0;
		double xLoc = 0.0;
		for (DgAnalysisPopulation p : quantiles){
			quantile++;
			xLoc = quantile /this.nQuantiles;
			xLoc*=100.0;
			DgModeSwitchPlanTypeAnalyzer modeSwitchAnalysis = new DgModeSwitchPlanTypeAnalyzer(p, runId1, runId2);
			DgAnalysisPopulation car2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.CAR));
			DgAnalysisPopulation pt2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.PT));
			DgAnalysisPopulation pt2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.CAR));
			DgAnalysisPopulation car2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.PT));
			if ((car2carPop  != null) && (car2carPop.getPersonData().size() >= threshold)) {
				avgUtil = car2carPop.calcAverageScoreDifference(runId1, runId2);
				car2carSeries.add(xLoc, avgUtil);
			}
			if ((pt2ptPop != null) && (pt2ptPop.getPersonData().size() >= threshold)){
				avgUtil = pt2ptPop.calcAverageScoreDifference(runId1, runId2);
				pt2ptSeries.add(xLoc, avgUtil);
			}
			if ((pt2carPop != null) && (pt2carPop.getPersonData().size() >= threshold)){
				avgUtil = pt2carPop.calcAverageScoreDifference(runId1, runId2);
				pt2carSeries.add(xLoc, avgUtil);
			}
			if ((car2ptPop != null) && (car2ptPop.getPersonData().size() >= threshold)) {
				avgUtil = car2ptPop.calcAverageScoreDifference(runId1, runId2);
				car2ptSeries.add(xLoc, avgUtil);
			}
		}
		XYSeriesCollection ds = new XYSeriesCollection();
		ds.addSeries(car2carSeries);
		ds.addSeries(pt2ptSeries);		
		ds.addSeries(pt2carSeries);		
		ds.addSeries(car2ptSeries);		
		return ds;
	}

	public JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		ValueAxis xAxis = this.axisBuilder.createValueAxis("% of Population Sorted by Income");
		xAxis.setRange(0.0, 102.0);
		ValueAxis yAxis = this.axisBuilder.createValueAxis("Delta Utils [Utils]");
		yAxis.setRange(-0.05, 0.3);
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(yAxis);
		
		DgColorScheme colorScheme = new DgColorScheme();
		
		XYItemRenderer renderer2;
		renderer2 = new XYLineAndShapeRenderer(true, true);
		plot.setDataset(0, this.dataset);
		for (int i = 0; i <= 3; i++){
			renderer2.setSeriesStroke(i, new BasicStroke(2.0f));
			renderer2.setSeriesOutlineStroke(i, new BasicStroke(3.0f));
			renderer2.setSeriesPaint(i, colorScheme.getColor(i+1, "a"));
		}
		plot.setRenderer(0, renderer2);
		
		JFreeChart chart = new JFreeChart("", plot);
		chart.setBackgroundPaint(ChartColor.WHITE);
		chart.getLegend().setItemFont(this.axisBuilder.getAxisFont());
		chart.setTextAntiAlias(true);
		return chart;
	}
	
	public XYSeriesCollection getDataset() {
		return dataset;
	}
}
