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
package playground.dgrether.analysis.population;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.population.PlanImpl.Type;
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.collections.Tuple;

import playground.dgrether.utils.DgChartUtils;



public class DgDeltaScoreIncomeModeChoiceChart {
	
	private static final Logger log = Logger.getLogger(DgDeltaScoreIncomeModeChoiceChart.class);
	
	private DgAnalysisPopulation ana;

	private boolean writeModeSwitcherOnly = false;
	
	public DgDeltaScoreIncomeModeChoiceChart(DgAnalysisPopulation ana) {
		this.ana = ana;
	}

	public void writeFile(String filename) {
		List<Tuple<Double, Double>> valuesCarCar = new ArrayList<Tuple<Double, Double>>();
		List<Tuple<Double, Double>> valuesPtPt = new ArrayList<Tuple<Double, Double>>();
		List<Tuple<Double, Double>> valuesPtCar = new ArrayList<Tuple<Double, Double>>();
		List<Tuple<Double, Double>> valuesCarPt = new ArrayList<Tuple<Double, Double>>();
		
		for (DgPersonData d : ana.getPersonData().values()) {
			DgPlanData planDataRun1 = d.getPlanData().get(DgAnalysisPopulation.RUNID1);
			DgPlanData planDataRun2 = d.getPlanData().get(DgAnalysisPopulation.RUNID2);
			Double scoreDiff = planDataRun2.getScore() - planDataRun1.getScore();
			Tuple<Double, Double> t = new Tuple<Double, Double>(d.getIncome().getIncome(), scoreDiff);
			
			if (planDataRun1.getPlan().getType().equals(Type.CAR) && planDataRun2.getPlan().getType().equals(Type.CAR)) {
				valuesCarCar.add(t);
			}
			else if (planDataRun1.getPlan().getType().equals(Type.PT) && planDataRun2.getPlan().getType().equals(Type.PT)) {
				valuesPtPt.add(t);
			}
			else if (planDataRun1.getPlan().getType().equals(Type.PT) && planDataRun2.getPlan().getType().equals(Type.CAR)) {
				if (d.getIncome().getIncome() > 100000.0 ) {
					log.info("PT->Car person id: " + d.getPersonId().toString() + " income " + d.getIncome().getIncome());
				}
				valuesPtCar.add(t);
			}
			else if (planDataRun1.getPlan().getType().equals(Type.CAR) && planDataRun2.getPlan().getType().equals(Type.PT)){
				if (d.getIncome().getIncome() > 100000.0 ) {
					log.info("Car->PT person id: " + d.getPersonId().toString() + " income " + d.getIncome().getIncome());
				}
				valuesCarPt.add(t);
			}
		}
		
		XYScatterChart chart = new XYScatterChart("", "Income", "Delta utils");

		Tuple<double[], double[]> data = DgChartUtils.createArray(valuesCarCar);
		double[] xvalues = data.getFirst();
		double[] yvalues = data.getSecond();
		if (!writeModeSwitcherOnly) {
			chart.addSeries("Car2Car", xvalues, yvalues);
		}

		data = DgChartUtils.createArray(valuesPtPt);
		xvalues = data.getFirst();
		yvalues = data.getSecond();
		if (!writeModeSwitcherOnly) {
				chart.addSeries("Pt2Pt", xvalues, yvalues);
		}
		
		data = DgChartUtils.createArray(valuesPtCar);
		xvalues = data.getFirst();
		yvalues = data.getSecond();
		chart.addSeries("Pt2Car", xvalues, yvalues);

		data = DgChartUtils.createArray(valuesCarPt);
		xvalues = data.getFirst();
		yvalues = data.getSecond();
		chart.addSeries("Car2Pt", xvalues, yvalues);
		
		
		chart.saveAsPng(filename, 800, 600);
		log.info("DeltaScoreIncomeChart written to : " +filename);
	}



	
	public boolean isWriteModeSwitcherOnly() {
		return writeModeSwitcherOnly;
	}



	
	public void setWriteModeSwitcherOnly(boolean writeModeSwitcherOnly) {
		this.writeModeSwitcherOnly = writeModeSwitcherOnly;
	}
	


}
