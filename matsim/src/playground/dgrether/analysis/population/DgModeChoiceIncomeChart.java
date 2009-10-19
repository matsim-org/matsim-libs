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

import org.apache.log4j.Logger;
import org.jfree.chart.JFreeChart;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.utils.charts.BarChart;



public class DgModeChoiceIncomeChart {
	
	private static final Logger log = Logger.getLogger(DgModeChoiceIncomeChart.class);
	
	private DgAnalysisPopulation ana;
	
	private int numberOfClasses = 10;

	
	public DgModeChoiceIncomeChart(DgAnalysisPopulation ana) {
		this.ana = ana;
		this.ana.calculateMinMaxIncome();
	}

	public void writeFile(String filename, Id runId) {
		// calculate thresholds for income classes
		DgIncomeClass[] incomeThresholds = new DgIncomeClass[this.numberOfClasses];
		DgAnalysisPopulation[] groups = new DgAnalysisPopulation[this.numberOfClasses];
		
		double deltaY = this.ana.getMaxIncome() / (this.numberOfClasses -1);
		for (int i = 0; i < incomeThresholds.length; i++) {
			incomeThresholds[i] = new DgIncomeClass(i *deltaY, i+1 * deltaY);
			groups[i] = new DgAnalysisPopulation();
		}
		
		for (DgPersonData d : ana.getPersonData().values()) {
			double y = d.getIncome().getIncome();
			int pos = (int) (y / deltaY);
			DgIncomeClass c = incomeThresholds[pos];
			if (!(c.getMin() <= y) && (y <= c.getMax())) {
				throw new IllegalStateException();
			}
			groups[pos].getPersonData().put(d.getPersonId(), d);
		}
		
		String[] groupDescriptions = new String[groups.length];
		double[] xvalues = new double[groups.length];
		double[] carvalues = new double[groups.length];
		double[] ptvalues = new double[groups.length];
		
		
		double groupSize = 0.0;
		double carPlans = 0.0;
		for (int i = 0; i < groups.length; i++) {
			groupDescriptions[i] = incomeThresholds[i].getTitle();
			xvalues[i] = i;
			groupSize = groups[i].getPersonData().size();
			carPlans = groups[i].calculateNumberOfCarPlans(runId);
			carvalues[i] = carPlans / groupSize * 100.0;
			ptvalues[i] = (groupSize - carPlans) / groupSize * 100.0;
		}
		
		
		BarChart chart = new BarChart("", "Income", "% of car drivers", groupDescriptions);


		chart.addSeries("car", carvalues);	
		chart.addSeries("non-car", ptvalues);
		
		chart.saveAsPng(filename, 1000, 600);
		log.info("ModeChoiceIncomeChart written to : " +filename);
	}

	
	public int getNumberOfClasses() {
		return numberOfClasses;
	}

	
	public void setNumberOfClasses(int numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}

	public JFreeChart createChart() {
		return null;
	}
	


}
