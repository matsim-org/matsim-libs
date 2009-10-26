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
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgIncomeClass;
import playground.dgrether.analysis.population.DgModeSwitchPlanTypeAnalyzer;
import playground.dgrether.analysis.population.DgPersonData;
import playground.dgrether.analysis.population.DgPlanData;
import playground.dgrether.utils.charts.DgColorScheme;



public class DgAvgDeltaScoreIncomeModeChoiceChart {
	
	private static final Logger log = Logger.getLogger(DgAvgDeltaScoreIncomeModeChoiceChart.class);
	
	private DgAnalysisPopulation ana;
	
	private int numberOfClasses = 11;
	
	private int groupThreshold = 3;
	
	private XYSeriesCollection dataset;
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();
	
	public DgAvgDeltaScoreIncomeModeChoiceChart(DgAnalysisPopulation ana) {
		this.ana = ana;
		this.ana.calculateMinMaxIncome();
		this.dataset = this.createDatasets();
	}

	
	private Double calcAverageScoreDifference(DgAnalysisPopulation group) {
		Double deltaScoreSum = 0.0;
		for (DgPersonData d : group.getPersonData().values()){
			DgPlanData planDataRun1 = d.getPlanData().get(DgAnalysisPopulation.RUNID1);
			DgPlanData planDataRun2 = d.getPlanData().get(DgAnalysisPopulation.RUNID2);
			deltaScoreSum += (planDataRun2.getScore() - planDataRun1.getScore());
		}
		Double avg = null;
		if (group.getPersonData().size() > groupThreshold) {
			avg = deltaScoreSum/group.getPersonData().size();
		}
		return avg;
	}
	
	private XYSeries createXYSeries(String title, DgAnalysisPopulation pop) {
		// calculate thresholds for income classes
		DgIncomeClass[] incomeThresholds = new DgIncomeClass[this.numberOfClasses];
		DgAnalysisPopulation[] groups = new DgAnalysisPopulation[this.numberOfClasses];

		double deltaY = this.ana.getMaxIncome() / (this.numberOfClasses -1);
		for (int i = 0; i < incomeThresholds.length; i++) {
			incomeThresholds[i] = new DgIncomeClass((i *deltaY), ((i+1) * deltaY));
			groups[i] = new DgAnalysisPopulation();
		}
		
		for (DgPersonData d : pop.getPersonData().values()) {
			double y = d.getIncome().getIncome();
			int pos = (int) (y / deltaY);
			DgIncomeClass c = incomeThresholds[pos];
			if (!(c.getMin() <= y) && (y <= c.getMax())) {
				throw new IllegalStateException();
			}
			groups[pos].getPersonData().put(d.getPersonId(), d);
		}

		XYSeries series = new XYSeries(title, false, true);
		for (int i = 0; i < groups.length; i++) {
//			groupDescriptions[i] = incomeThresholds[i].title;
//			xvalues[i] = incomeThresholds[i].max;
//			yvalues[i] = calcAverageScoreDifference(groups[i]);
			Double avgScore = calcAverageScoreDifference(groups[i]);
			if (avgScore != null) {
			  double incomeLocation = incomeThresholds[i].getMin() + (deltaY / 2.0);
			  series.add(incomeLocation, avgScore);
			}
		}
		return series;
	}
	

	private XYSeriesCollection createDatasets() {
		DgModeSwitchPlanTypeAnalyzer modeSwitchAnalysis = new DgModeSwitchPlanTypeAnalyzer(this.ana);
		DgAnalysisPopulation car2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.CAR));
		DgAnalysisPopulation pt2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.PT));
		DgAnalysisPopulation pt2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.CAR));
		DgAnalysisPopulation car2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.PT));
		XYSeriesCollection ds = new XYSeriesCollection();
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Car2Car", car2carPop));
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Pt2Pt", pt2ptPop));
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Pt2Car", pt2carPop));
		ds.addSeries(this.createXYSeries("Mean "+  '\u0394' + "Utility Car2Pt", car2ptPop));		
		return ds;
	}

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
	
	public XYSeriesCollection getDataset() {
		return dataset;
	}
	
	public int getNumberOfClasses() {
		return numberOfClasses;
	}

	public void setNumberOfClasses(int numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}

}
