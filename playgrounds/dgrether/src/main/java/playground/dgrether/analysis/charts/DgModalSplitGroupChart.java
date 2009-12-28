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
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.api.core.v01.Id;

import playground.dgrether.analysis.charts.utils.DgColorScheme;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgIncomeClass;
import playground.dgrether.analysis.population.DgPersonData;



public class DgModalSplitGroupChart {
	
	private static final Logger log = Logger.getLogger(DgModalSplitGroupChart.class);
	
	private DgAnalysisPopulation ana;
	
	private int numberOfClasses = 10;
	
	private int groupThreshold = 0;
	
	private DefaultCategoryDataset dataset;
	
	private String xLabel = "Income [CHF / Year] up to...";
	private String yLabel = "% of Travellers";

	private Id runId; 
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();
	
	public DgModalSplitGroupChart(DgAnalysisPopulation ana, Id runid, int threshold) {
		this.ana = ana;
		this.runId = runid;
		this.groupThreshold = threshold;
		this.ana.calculateMinMaxIncome();
		this.dataset = new DefaultCategoryDataset();
		this.calculateData();
	}
	
	private void calculateData() {
		// calculate thresholds for income classes
		DgIncomeClass[] incomeThresholds = new DgIncomeClass[this.numberOfClasses];
		DgAnalysisPopulation[] groups = new DgAnalysisPopulation[this.numberOfClasses];
		
		double deltaY = this.ana.getMaxIncome() / (this.numberOfClasses -1);
		for (int i = 0; i < incomeThresholds.length; i++) {
			incomeThresholds[i] = new DgIncomeClass((i *deltaY), ((i+1) * deltaY));
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
//			groupDescriptions[i] = incomeThresholds[i].getTitle();
			String title = Double.toString(incomeThresholds[i].getMax());
			title = title.substring(0, (title.indexOf(".") /*+ 3*/));
			groupDescriptions[i] = title;
			xvalues[i] = i;
			groupSize = groups[i].getPersonData().size();
			if (groupSize < this.groupThreshold) {
				continue;
			}
			carPlans = groups[i].calculateNumberOfCarPlans(runId);
			carvalues[i] = carPlans / groupSize * 100.0;
			ptvalues[i] = (groupSize - carPlans) / groupSize * 100.0;
		
			this.dataset.addValue(carvalues[i], "car", groupDescriptions[i]);
			this.dataset.addValue(ptvalues[i], "non-car", groupDescriptions[i]);
		}
	}

	public JFreeChart createChart() {
		CategoryAxis categoryAxis = this.axisBuilder.createCategoryAxis(xLabel);
		categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
		ValueAxis valueAxis = this.axisBuilder.createValueAxis(yLabel);
		valueAxis.setRange(0.0, 100.0);
		
		DgColorScheme colorScheme = new DgColorScheme();

		CategoryPlot plot = new CategoryPlot();
		plot.setDomainAxis(categoryAxis);
		plot.setRangeAxis(valueAxis);
		plot.setDataset(0, this.dataset);
		BarRenderer carRenderer = new BarRenderer();
		carRenderer.setSeriesPaint(0, colorScheme.COLOR1A);
		carRenderer.setSeriesPaint(1, colorScheme.COLOR3A);
	
		carRenderer.setItemMargin(0.10);
		plot.setRenderer(0, carRenderer);
		
		JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		chart.setBackgroundPaint(ChartColor.WHITE);
		chart.getLegend().setItemFont(this.axisBuilder.getAxisFont());
		return chart;
	}
	
	public int getNumberOfClasses() {
		return numberOfClasses;
	}
	
	public void setNumberOfClasses(int numberOfClasses) {
		this.numberOfClasses = numberOfClasses;
	}


}
