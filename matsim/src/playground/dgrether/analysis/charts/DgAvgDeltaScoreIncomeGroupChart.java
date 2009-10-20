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

import org.apache.log4j.Logger;
import org.matsim.core.utils.charts.XYScatterChart;

import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonData;
import playground.dgrether.analysis.population.DgPlanData;



public class DgAvgDeltaScoreIncomeGroupChart {
	
	private static final Logger log = Logger.getLogger(DgAvgDeltaScoreIncomeGroupChart.class);
	
	private DgAnalysisPopulation ana;
	
	private int numberOfClasses = 10;

	private double minIncome = Double.POSITIVE_INFINITY;
	private double maxIncome = Double.NEGATIVE_INFINITY;
	
	public DgAvgDeltaScoreIncomeGroupChart(DgAnalysisPopulation ana) {
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
		
		
		XYScatterChart chart = new XYScatterChart("", "Income groups", "Delta Utility");

		chart.addSeries("avg delta utility", xvalues, yvalues);	
		
		chart.saveAsPng(filename, 1000, 600);
		log.info("DgAvgDeltaScoreIncomeGroupChart written to : " +filename);
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

	public int getNumberOfClasses() {
		return numberOfClasses;
	}

	
	public void setNumberOfClasses(int numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}
	


}
