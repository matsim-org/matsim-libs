/* *********************************************************************** *
 * project: org.matsim.*
 * DgModalSplitQuantilesChart
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
import playground.dgrether.analysis.population.DgPersonDataIncomeComparator;

/**
 * @author dgrether
 */
public class DgModalSplitQuantilesChart {
	private DgAnalysisPopulation ana;
	
	protected int nQuantiles = 10;
	
	private DefaultCategoryDataset dataset;
	
	private String xLabel = "Income [CHF / Year] up to...";
	private String yLabel = "% of Travellers";

	private Id runId; 
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();
	
	public DgModalSplitQuantilesChart(DgAnalysisPopulation ana, Id runid) {
		this.ana = ana;
		this.runId = runid;
		this.ana.calculateMinMaxIncome();
		this.dataset = new DefaultCategoryDataset();
		this.calculateData();
	}
	
	private void calculateData() {
		List<DgAnalysisPopulation> quantiles = this.ana.getQuantiles(this.nQuantiles, new DgPersonDataIncomeComparator());
		List<String> labels = new ArrayList<String>();
		
		for (DgAnalysisPopulation p : quantiles){
			p.calculateMinMaxIncome();
			double carPlans = p.calculateNumberOfCarPlans(runId);
			int groupSize = p.getPersonData().size();
			double carFraction =  carPlans / groupSize * 100.0;
			double ptFraction = (groupSize - carPlans) / groupSize * 100;
			String title = Double.toString(p.getMaxIncome());
			title = title.substring(0, title.indexOf("."));
			this.dataset.addValue(carFraction, "car", title);
			this.dataset.addValue(ptFraction, "non-car", title);
		}
	}

	public JFreeChart createChart() {
		CategoryAxis categoryAxis = this.axisBuilder.createCategoryAxis(xLabel);
		categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
		ValueAxis valueAxis = this.axisBuilder.createValueAxis(yLabel);
		valueAxis.setRange(0.0, 102.0);
		
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
		chart.removeLegend();
		return chart;
	}
	
}
