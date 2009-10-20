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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
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



public class DgAvgDeltaScoreIncomeModeChoiceChart {
	
	private static final Logger log = Logger.getLogger(DgAvgDeltaScoreIncomeModeChoiceChart.class);
	
	private DgAnalysisPopulation ana;
	
	private int numberOfClasses = 11;
	
	private int groupThreshold = 3;
	
	private List<XYSeriesCollection> datasets;
	
	public DgAvgDeltaScoreIncomeModeChoiceChart(DgAnalysisPopulation ana) {
		this.ana = ana;
		this.ana.calculateMinMaxIncome();
		this.datasets = this.createDatasets();
	}

	private XYSeries createSeries(final String title, List<Tuple<Double, Double>> values) {
		XYSeries series = new XYSeries(title, false, true);
		for (Tuple<Double, Double> t : values) {
			series.add(t.getFirst(), t.getSecond());
		}
		return series;
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

	private List<XYSeriesCollection> createDatasets() {
		List<XYSeriesCollection> dss = new ArrayList<XYSeriesCollection>();
		List<DgAnalysisPopulation> popsPerModeSwitch = new ArrayList<DgAnalysisPopulation>();
		DgModeSwitchPlanTypeAnalyzer modeSwitchAnalysis = new DgModeSwitchPlanTypeAnalyzer(this.ana);
		DgAnalysisPopulation car2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.CAR));
		DgAnalysisPopulation pt2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.PT));
		DgAnalysisPopulation pt2carPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.PT, PlanImpl.Type.CAR));
		DgAnalysisPopulation car2ptPop = modeSwitchAnalysis.getPersonsForModeSwitch(new Tuple(PlanImpl.Type.CAR, PlanImpl.Type.PT));
		popsPerModeSwitch.add(car2carPop);
		popsPerModeSwitch.add(pt2ptPop);
		popsPerModeSwitch.add(pt2carPop);
		popsPerModeSwitch.add(car2ptPop);
//		
		for (DgAnalysisPopulation population : popsPerModeSwitch) {
			// calculate thresholds for income classes
			DgIncomeClass[] incomeThresholds = new DgIncomeClass[this.numberOfClasses];
			DgAnalysisPopulation[] groups = new DgAnalysisPopulation[this.numberOfClasses];

			double deltaY = this.ana.getMaxIncome() / (this.numberOfClasses -1);
			for (int i = 0; i < incomeThresholds.length; i++) {
				incomeThresholds[i] = new DgIncomeClass((i *deltaY), ((i+1) * deltaY));
				groups[i] = new DgAnalysisPopulation();
			}
			
			for (DgPersonData d : population.getPersonData().values()) {
				double y = d.getIncome().getIncome();
				int pos = (int) (y / deltaY);
				DgIncomeClass c = incomeThresholds[pos];
				if (!(c.getMin() <= y) && (y <= c.getMax())) {
					throw new IllegalStateException();
				}
				groups[pos].getPersonData().put(d.getPersonId(), d);
			}
			
			List<Tuple<Double, Double>> values = new ArrayList<Tuple<Double, Double>>();
			for (int i = 0; i < groups.length; i++) {
//				groupDescriptions[i] = incomeThresholds[i].title;
//				xvalues[i] = incomeThresholds[i].max;
//				yvalues[i] = calcAverageScoreDifference(groups[i]);
				Double avgScore = calcAverageScoreDifference(groups[i]);
				if (avgScore != null) {
				  double incomeLocation = incomeThresholds[i].getMin() + (deltaY / 2.0);
					values.add(new Tuple<Double, Double>(incomeLocation, avgScore));
				}
			}
			
			XYSeriesCollection dataset = new XYSeriesCollection();
			XYSeries series = this.createSeries("avg delta utility", values);
			dataset.addSeries(series);
			dss.add(dataset);
		}
		return dss;
	}

	public JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		plot.setDomainAxis(new NumberAxis("Income groups"));
		plot.setRangeAxis(new NumberAxis("Delta Utility"));
		for (int i = 0; i < this.datasets.size(); i++){
			XYItemRenderer renderer = new XYLineAndShapeRenderer(true, true);
			plot.setDataset(i, this.datasets.get(i));
			plot.setRenderer(i, renderer);
		}
		
		JFreeChart jchart = new JFreeChart("", plot);
		jchart.setBackgroundPaint(ChartColor.WHITE);
		return jchart;
	}
	
	public List<XYSeriesCollection> getDatasets() {
		return datasets;
	}
	
	public int getNumberOfClasses() {
		return numberOfClasses;
	}

	public void setNumberOfClasses(int numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}

}
