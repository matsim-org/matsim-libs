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

import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeriesCollection;


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
		plot.setDomainAxis(new NumberAxis("x"));
		plot.setRangeAxis(new NumberAxis("y"));

		int i = 0;
		XYItemRenderer renderer2;
		for (XYSeriesCollection col : this.avgDeltaScoreIncomeDs){
			i++;
			plot.setDataset(i, col);
			renderer2 = new XYLineAndShapeRenderer(true, true);
			plot.setRenderer(i, renderer2);
		}

		
		XYItemRenderer renderer1 = new XYLineAndShapeRenderer(false, true);
		plot.setDataset(0, this.inomeModeChoiceDs);
		plot.setRenderer(0, renderer1);
		
		
		JFreeChart chart = new JFreeChart("Test",plot);
		return chart;
	}

}
