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

import org.apache.log4j.Logger;
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.analysis.charts.utils.DgColorScheme;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgModeSwitchPlanTypeAnalyzer;
/**
 * 
 * @author dgrether
 */
public class DgAvgDeltaUtilsModeGroupChart extends DgAvgDeltaUtilsGroupChart {
	
	private static final Logger log = Logger.getLogger(DgAvgDeltaUtilsModeGroupChart.class);
	
	public DgAvgDeltaUtilsModeGroupChart(DgAnalysisPopulation ana, int threshold) {
		super(ana, threshold);
	}

	@Override
	protected XYSeriesCollection createDatasets() {
		DgModeSwitchPlanTypeAnalyzer modeSwitchAnalysis = new DgModeSwitchPlanTypeAnalyzer(this.ana);
		DgAnalysisPopulation car2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.CAR));
		DgAnalysisPopulation pt2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.PT));
		DgAnalysisPopulation pt2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.CAR));
		DgAnalysisPopulation car2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.PT));
		XYSeriesCollection ds = new XYSeriesCollection();
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Car2Car", car2carPop).getFirst());
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Pt2Pt", pt2ptPop).getFirst());
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Pt2Car", pt2carPop).getFirst());
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Car2Pt", car2ptPop).getFirst());		
		return ds;
	}

	@Override
	public JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		ValueAxis xAxis = this.axisBuilder.createValueAxis("Income [Chf / Year]");
		ValueAxis yAxis = this.axisBuilder.createValueAxis("Delta Utils [Utils]");
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
	


}
