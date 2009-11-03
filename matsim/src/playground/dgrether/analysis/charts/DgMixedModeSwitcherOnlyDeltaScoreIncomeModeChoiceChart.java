/* *********************************************************************** *
 * project: org.matsim.*
 * DgMixedDeltaScoreIncomeChart
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

import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;

import playground.dgrether.analysis.charts.utils.DgColorScheme;


/**
 * @author dgrether
 *
 */
public class DgMixedModeSwitcherOnlyDeltaScoreIncomeModeChoiceChart {

	private XYSeriesCollection avgDeltaScoreIncomeDs;
	private XYSeriesCollection inomeModeChoiceDs;
	
	private DgAxisBuilder axisBuilder = new DgDefaultAxisBuilder();

	public void addAvgDeltaScoreIncomeDs(XYSeriesCollection dataset) {
		this.avgDeltaScoreIncomeDs = dataset;
	}

	public void addIncomeModeChoiceDataSet(XYSeriesCollection incomeModeChoice) {
		this.inomeModeChoiceDs = incomeModeChoice;
	}

	public JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		ValueAxis xAxis = this.axisBuilder.createValueAxis("Income [Chf / Year]");
		ValueAxis yAxis = this.axisBuilder.createValueAxis("Delta Utils [Utils]");
		plot.setDomainAxis(xAxis);
		plot.setRangeAxis(yAxis);
		
		DgColorScheme colorScheme = new DgColorScheme();
		
		XYItemRenderer renderer1 = new XYLineAndShapeRenderer(false, true);
		renderer1.setSeriesPaint(0, colorScheme.COLOR3B);
		renderer1.setSeriesPaint(1, colorScheme.COLOR4B);
		plot.setDataset(0, this.inomeModeChoiceDs);
		plot.setRenderer(0, renderer1);
		
		XYItemRenderer renderer2;
		renderer2 = new XYLineAndShapeRenderer(true, true);
		plot.setDataset(1, this.avgDeltaScoreIncomeDs);
		for (int i = 2; i <= 3; i++){
			renderer2.setSeriesStroke(i-2, new BasicStroke(2.0f));
			renderer2.setSeriesOutlineStroke(i-2, new BasicStroke(3.0f));
			renderer2.setSeriesPaint(i-2, colorScheme.getColor(i+1, "a"));
		}
		plot.setRenderer(1, renderer2);
		JFreeChart chart = new JFreeChart("",plot);
		chart.setBackgroundPaint(ChartColor.WHITE);
		chart.getLegend().setItemFont(this.axisBuilder.getAxisFont());
		chart.setTextAntiAlias(true);
		return chart;
	}

}
