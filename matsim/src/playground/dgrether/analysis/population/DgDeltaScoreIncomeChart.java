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
import org.matsim.core.utils.charts.XYScatterChart;
import org.matsim.core.utils.collections.Tuple;

import playground.benjamin.income.ChartData;
import playground.benjamin.income.ChartDataWriter;
import playground.dgrether.utils.DgChartUtils;

public class DgDeltaScoreIncomeChart {
	
	private static final Logger log = Logger.getLogger(DgDeltaScoreIncomeChart.class);
	
	private DgAnalysisPopulation ana;

	
	private int numberOfClasses = 10;

	private double minIncome = Double.POSITIVE_INFINITY;
	private double maxIncome = Double.NEGATIVE_INFINITY;
	
	private boolean writeModeSwitcherOnly = false;
	
	public DgDeltaScoreIncomeChart(DgAnalysisPopulation ana) {
		this.ana = ana;
		this.calculateMinMaxIncome();
	}

	private void calculateMinMaxIncome() {
		double y;
		for (DgPersonData d : ana.getPersonData().values()) {
			y = d.getIncome().getIncome();
			if (y< minIncome) {
				this.minIncome = y;
			}
			
			if (y > maxIncome) {
				this.maxIncome = y;
			}
			
		}
	}

	private static final class IncomeClass {		
		double min;
		double max;
		String title;
		
		public IncomeClass(double min, double max){
			this.min = min; 
			this.max = max;
			this.title = this.min + " - " + this.max;
		}
		
	}		


	public void writeFile(String filename) {
		List<Tuple<Double, Double>> valuesCarCar = new ArrayList<Tuple<Double, Double>>();
		
		for (DgPersonData d : ana.getPersonData().values()) {
			DgPlanData planDataRun1 = d.getPlanData().get(DgAnalysisPopulation.RUNID1);
			DgPlanData planDataRun2 = d.getPlanData().get(DgAnalysisPopulation.RUNID2);
			Double scoreDiff = planDataRun2.getScore() - planDataRun1.getScore();
			Tuple<Double, Double> t = new Tuple<Double, Double>(d.getIncome().getIncome(), scoreDiff);
			
				valuesCarCar.add(t);
		}
    //average values
		// calculate thresholds for income classes
		IncomeClass[] incomeThresholds = new IncomeClass[this.numberOfClasses];
		DgAnalysisPopulation[] groups = new DgAnalysisPopulation[this.numberOfClasses];
		
		double deltaY = this.maxIncome / (this.numberOfClasses -1);
		for (int i = 0; i < incomeThresholds.length; i++) {
			incomeThresholds[i] = new IncomeClass(i *deltaY, i+1 * deltaY);
			groups[i] = new DgAnalysisPopulation();
		}
		
		for (DgPersonData d : ana.getPersonData().values()) {
			double y = d.getIncome().getIncome();
			int pos = (int) (y / deltaY);
			IncomeClass c = incomeThresholds[pos];
			if (!(c.min <= y) && (y <= c.max)) {
				throw new IllegalStateException();
			}
			groups[pos].getPersonData().put(d.getPersonId(), d);
		}
		
		String[] groupDescriptions = new String[groups.length];
		double[] xvalues = new double[groups.length];
		double[] yvalues = new double[groups.length];
		

		for (int i = 0; i < groups.length; i++) {
			groupDescriptions[i] = incomeThresholds[i].title;
			xvalues[i] = incomeThresholds[i].max;
			yvalues[i] = calcAverageScoreDifference(groups[i]);
		}
		
		
		
		
		
		//create chart
		
		XYScatterChart chart = new XYScatterChart("", "Income", "Delta utils");

		Tuple<double[], double[]> data = DgChartUtils.createArray(valuesCarCar);
		double[] xvalues1 = data.getFirst();
		double[] yvalues1 = data.getSecond();
		chart.addSeries("Individual utility differences", xvalues1, yvalues1);
//		chart.addSeries("Average utility differences", xvalues, yvalues);
		ChartData filedata = new ChartData("Utility differences", "Income", "Delta utils");
		filedata.addSeries("average utility differences", xvalues, yvalues);
		filedata.addSeries("Individual utility differences", xvalues1, yvalues1);
		new ChartDataWriter(filedata).writeFile(filename.substring(0, filename.length() -4) + ".txt");
		
		
		
		chart.saveAsPng(filename, 800, 600);
		log.info("DeltaScoreIncomeChart written to : " +filename);
	}


	private double calcAverageScoreDifference(DgAnalysisPopulation group) {
		double deltaScoreSum = 0.0;
		for (DgPersonData d : group.getPersonData().values()){
			DgPlanData planDataRun1 = d.getPlanData().get(DgAnalysisPopulation.RUNID1);
			DgPlanData planDataRun2 = d.getPlanData().get(DgAnalysisPopulation.RUNID2);
			deltaScoreSum += planDataRun2.getScore() - planDataRun1.getScore();
		}
		return deltaScoreSum/group.getPersonData().size();
	}
	
	public boolean isWriteModeSwitcherOnly() {
		return writeModeSwitcherOnly;
	}



	
	public void setWriteModeSwitcherOnly(boolean writeModeSwitcherOnly) {
		this.writeModeSwitcherOnly = writeModeSwitcherOnly;
	}
	


}
