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
package playground.dgrether.analysis;

import java.awt.BasicStroke;
import java.util.List;

import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;

import playground.dgrether.utils.charts.DgColorScheme;


/**
 * @author dgrether
 *
 */
public class DgMixedDeltaScoreIncomeChart {

	private List<XYSeriesCollection> avgDeltaScoreIncomeDs;
	private XYSeriesCollection inomeModeChoiceDs;

	public void addAvgDeltaScoreIncomeDs(List<XYSeriesCollection> datasets) {
		this.avgDeltaScoreIncomeDs = datasets;
	}

	public void addIncomeModeChoiceDataSet(XYSeriesCollection incomeModeChoice) {
		this.inomeModeChoiceDs = incomeModeChoice;
	}

	public JFreeChart createChart() {
		XYPlot plot = new XYPlot();
		plot.setDomainAxis(new NumberAxis("Income [Chf / Year]"));
		plot.setRangeAxis(new NumberAxis("Delta utils [Utils]"));
		
		DgColorScheme colorScheme = new DgColorScheme();
		
		XYItemRenderer renderer1 = new XYLineAndShapeRenderer(false, true);
		renderer1.setSeriesPaint(0, colorScheme.COLOR1B);
		renderer1.setSeriesPaint(1, colorScheme.COLOR2B);
		renderer1.setSeriesPaint(2, colorScheme.COLOR3B);
		renderer1.setSeriesPaint(3, colorScheme.COLOR4B);
		plot.setDataset(0, this.inomeModeChoiceDs);
		plot.setRenderer(0, renderer1);
		
		int i = 0;
		XYItemRenderer renderer2;
		for (XYSeriesCollection col : this.avgDeltaScoreIncomeDs){
			i++;
			plot.setDataset(i, col);
			renderer2 = new XYLineAndShapeRenderer(true, true);
			renderer2.setSeriesStroke(0, new BasicStroke(2.0f));
			renderer2.setSeriesOutlineStroke(0, new BasicStroke(3.0f));
			renderer2.setSeriesPaint(0, colorScheme.getColor(i, "a"));
			plot.setRenderer(i, renderer2);
		}

		JFreeChart chart = new JFreeChart("Test",plot);
		chart.setBackgroundPaint(ChartColor.WHITE);
		return chart;
	}

}
