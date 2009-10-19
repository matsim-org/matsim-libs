/* *********************************************************************** *
 * project: org.matsim.*
 * MixedChartTest
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
package playground.dgrether.utils.charts;

import java.awt.Color;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * @author dgrether
 *
 */
public class MixedChartTest {

	public MixedChartTest() {
		//Create first dataset
		XYSeriesCollection data1 = new XYSeriesCollection();
		XYSeries series = new XYSeries("xy series", false, true);
		for (int x = 1; x < 10; x++) {
//			for (int y = 1; y < 10; y++) {
				series.add(x, x);
//			}
		}
		data1.addSeries(series);
		
		//Create first Renderer
		XYItemRenderer renderer1 = new XYLineAndShapeRenderer(true, true);
		//Create plot
		XYPlot plot = new XYPlot();
		plot.setDataset(0, data1);
		plot.setRenderer(0, renderer1);
		plot.setDomainAxis(new NumberAxis("x"));
		plot.setRangeAxis(new NumberAxis("y"));
		
//		plot.setDomainAxis(new CategoryAxis("Spalte"));
//		plot.setRangeAxis(new NumberAxis("Value"));
		plot.setOrientation(PlotOrientation.VERTICAL);
		
		//create second dataset
		XYSeriesCollection data2 = new XYSeriesCollection();
		XYSeries series2 = new XYSeries("xy series2", false, true);
		for (int x = 10; x > 0; x--) {
			for (int y = 10; y > 0; y--) {
				series2.add(x, x*y);
			}
		}
		data2.addSeries(series2);
		//create second renderer
		XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(false, true);
		renderer2.setSeriesPaint(0, Color.GREEN);
		//Add second dataset and renderer to the previously created plot
		plot.setDataset(1, data2);
		plot.setRenderer(1, renderer2);
		
		//Create chart
		JFreeChart chart = new JFreeChart("Test",plot);
		DgChartFrame frame = new DgChartFrame("test", chart);
	}
	
	
	
	public static void main(String[] args) {
		new MixedChartTest();
	}
}
