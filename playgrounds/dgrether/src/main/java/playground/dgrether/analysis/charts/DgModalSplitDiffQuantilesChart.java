/* *********************************************************************** *
 * project: org.matsim.*
 * DgModalSplitDiffQuantilesChart
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

import java.awt.Font;
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

import playground.dgrether.analysis.charts.utils.DgCategoryLabelGenerator;
import playground.dgrether.analysis.charts.utils.DgColorScheme;
import playground.dgrether.analysis.population.DgAnalysisPopulation;
import playground.dgrether.analysis.population.DgPersonDataIncomeComparator;


/**
 * @author dgrether
 *
 */
public class DgModalSplitDiffQuantilesChart {
	private DgAnalysisPopulation ana;
	
	protected int nQuantiles = 10;
	
	private DefaultCategoryDataset dataset;
	
	private String xLabel = "Income [CHF / Year] up to...";
	private String yLabel = "% of Travellers";

	private Id runId1; 
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();

	private Id runId2;

	private DgCategoryLabelGenerator labelgenerator;
	
	public DgModalSplitDiffQuantilesChart(DgAnalysisPopulation ana, Id runid1, Id runid2) {
		this.ana = ana;
		this.runId1 = runid1;
		this.runId2 = runid2;
		this.ana.calculateMinMaxIncome();
		this.dataset = new DefaultCategoryDataset();
		this.labelgenerator = new DgCategoryLabelGenerator();
		this.calculateData();
	}
	
	private void calculateData() {
		List<DgAnalysisPopulation> quantiles = this.ana.getQuantiles(this.nQuantiles, new DgPersonDataIncomeComparator());
		List<String> labels = new ArrayList<String>();
		int column = 0;
		for (DgAnalysisPopulation p : quantiles){
			p.calculateMinMaxIncome();
			double carPlans1 = p.calculateNumberOfCarPlans(runId1);
			double carPlans2 = p.calculateNumberOfCarPlans(runId2);
			int groupSize = p.getPersonData().size();
			double carFraction =  (carPlans2 - carPlans1) / groupSize * 100.0;
			double ptFraction = (carPlans1- carPlans2) / groupSize * 100;
			String title = Double.toString(p.getMaxIncome());
			title = title.substring(0, title.indexOf("."));
			this.dataset.addValue(carFraction, "car", title);
			this.dataset.addValue(ptFraction, "non-car", title);
			if (carFraction != 0.0) {
				String s = Double.toString(carFraction);
				this.labelgenerator.addLabel(0, column, s.substring(0, s.lastIndexOf(".")+2));
				s = Double.toString(ptFraction);
				this.labelgenerator.addLabel(1, column, s.substring(0, s.lastIndexOf(".")+2));
			}
			column++;
		}
	}

	public JFreeChart createChart() {
		CategoryAxis categoryAxis = this.axisBuilder.createCategoryAxis(xLabel);
		categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_45);
		ValueAxis valueAxis = this.axisBuilder.createValueAxis(yLabel);
		//RANGE
//		valueAxis.setRange(-50.0, 50.0); //test
		valueAxis.setRange(-20.0, 20.0); //zh
		DgColorScheme colorScheme = new DgColorScheme();

		CategoryPlot plot = new CategoryPlot();
		plot.setDomainAxis(categoryAxis);
//		plot.setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
		plot.setRangeAxis(valueAxis);
		plot.setDataset(0, this.dataset);
//		plot.setDomainGridlinePosition(CategoryAnchor.END);
//		plot.setDomainGridlinesVisible(true);
		BarRenderer carRenderer = new BarRenderer();
		carRenderer.setSeriesPaint(0, colorScheme.COLOR1A);
		carRenderer.setSeriesPaint(1, colorScheme.COLOR3A);
		carRenderer.setSeriesItemLabelGenerator(0, this.labelgenerator);
		carRenderer.setSeriesItemLabelGenerator(1, this.labelgenerator);
		Font labelFont = new Font("Helvetica", Font.BOLD, 14);
		carRenderer.setSeriesItemLabelFont(0, labelFont);
		carRenderer.setSeriesItemLabelFont(1, labelFont);
		carRenderer.setSeriesItemLabelsVisible(0, true);
		carRenderer.setSeriesItemLabelsVisible(1, true);
		
		carRenderer.setItemMargin(0.15);
		plot.setRenderer(0, carRenderer);
		
		JFreeChart chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		chart.setBackgroundPaint(ChartColor.WHITE);
		chart.removeLegend();
//		chart.getLegend().setItemFont(this.axisBuilder.getAxisFont());
		return chart;
	}

	
}
