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
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PlanImpl.Type;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.analysis.charts.interfaces.DgChart;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonData;
import playground.dgrether.analysis.population.DgPersonDataIncomeComparator;
import playground.dgrether.analysis.population.DgPlanData;



public class DgDeltaUtilsModeGroupChart implements DgChart {
	
	private static final Logger log = Logger.getLogger(DgDeltaUtilsModeGroupChart.class);
	
	private DgAnalysisPopulation ana;
	
	private boolean writeModeSwitcherOnly = false;
	
	private XYSeriesCollection dataset;
	
	public DgDeltaUtilsModeGroupChart(DgAnalysisPopulation ana) {
		this.ana = ana;
		this.dataset = createDeltaScoreIncomeModeChoiceDataset();
	}

	public XYSeriesCollection createDeltaScoreIncomeModeChoiceDataset() {
		List<Tuple<Double, Double>> valuesCarCar = new ArrayList<Tuple<Double, Double>>();
		List<Tuple<Double, Double>> valuesPtPt = new ArrayList<Tuple<Double, Double>>();
		List<Tuple<Double, Double>> valuesPtCar = new ArrayList<Tuple<Double, Double>>();
		List<Tuple<Double, Double>> valuesCarPt = new ArrayList<Tuple<Double, Double>>();
		List<DgPersonData> sortedPop = new ArrayList<DgPersonData>();
		sortedPop.addAll(this.ana.getPersonData().values());
		Collections.sort(sortedPop, new DgPersonDataIncomeComparator());		
		//calculations
//		double currentIncome = 0.0;
		double i = 0.0;
		double size = this.ana.getPersonData().size();
		for (DgPersonData d : sortedPop) {
			i++;
//			currentIncome += d.getIncome().getIncome();
			DgPlanData planDataRun1 = d.getPlanData().get(DgAnalysisPopulation.RUNID1);
			DgPlanData planDataRun2 = d.getPlanData().get(DgAnalysisPopulation.RUNID2);
			Double scoreDiff = planDataRun2.getScore() - planDataRun1.getScore();
			Tuple<Double, Double> t = new Tuple<Double, Double>(100.0*i/size, scoreDiff);
			
			if (((PlanImpl) planDataRun1.getPlan()).getType().equals(Type.CAR) && ((PlanImpl) planDataRun2.getPlan()).getType().equals(Type.CAR)) {
				valuesCarCar.add(t);
			}
			else if (((PlanImpl) planDataRun1.getPlan()).getType().equals(Type.PT) && ((PlanImpl) planDataRun2.getPlan()).getType().equals(Type.PT)) {
				valuesPtPt.add(t);
			}
			else if (((PlanImpl) planDataRun1.getPlan()).getType().equals(Type.PT) && ((PlanImpl) planDataRun2.getPlan()).getType().equals(Type.CAR)) {
				valuesPtCar.add(t);
			}
			else if (((PlanImpl) planDataRun1.getPlan()).getType().equals(Type.CAR) && ((PlanImpl) planDataRun2.getPlan()).getType().equals(Type.PT)){
				valuesCarPt.add(t);
			}
		}
		
		//compose the dataset
		XYSeriesCollection ds = new XYSeriesCollection();
		if (!writeModeSwitcherOnly) {
			ds.addSeries(this.createSeries("Car2Car", valuesCarCar));
		}
		if (!writeModeSwitcherOnly) {
			ds.addSeries(this.createSeries("Pt2Pt", valuesPtPt));
		}
		ds.addSeries(this.createSeries("Pt2Car", valuesPtCar));
		ds.addSeries(this.createSeries("Car2Pt", valuesCarPt));
		return ds;
	}
	
	/**
	 * @see playground.dgrether.analysis.charts.interfaces.DgChart#createChart()
	 */
	public JFreeChart createChart() {
		XYPlot plot = new XYPlot(this.dataset, new NumberAxis("Income"), new NumberAxis("Delta utils"), null);
		XYItemRenderer renderer = new XYLineAndShapeRenderer(false, true);
		plot.setRenderer(renderer);
		
		JFreeChart jchart = new JFreeChart("", plot);
		return jchart;
	}

	private XYSeries createSeries(final String title, List<Tuple<Double, Double>> values) {
		XYSeries series = new XYSeries(title, false, true);
		for (Tuple<Double, Double> t : values) {
			series.add(t.getFirst(), t.getSecond());
		}
		return series;
	}
	
	public boolean isWriteModeSwitcherOnly() {
		return writeModeSwitcherOnly;
	}

	public void setWriteModeSwitcherOnly(boolean writeModeSwitcherOnly) {
		this.writeModeSwitcherOnly = writeModeSwitcherOnly;
	}
	


}
