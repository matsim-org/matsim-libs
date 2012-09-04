/* *********************************************************************** *
 * project: org.matsim.*
 * DepartureTimeDistributionChart.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.grips.complexodpopulationgenerator;

import org.jfree.ui.DrawablePanel;
import org.matsim.core.utils.charts.XYLineChart;



public class DepartureTimeDistributionChart extends DrawablePanel{
	
	
	private final static int STEPS = 50;
	

	public DepartureTimeDistributionChart(){
		
//		g.drawLine(0, 0, 180, 180);
		XYLineChart chart = new XYLineChart("Departure time distribution", "time in h", "");
		double [] xs = {0, 1, 2, 3, 4, 5};
		double [] ys = {0,1,2,3,4,5};
		chart.addSeries("model", xs, ys);
		setDrawable(chart.getChart());
	}
	
	public void createChart(double earliest, double latest, double sigma, double mu) {
		XYLineChart chart = new XYLineChart("Departure time distribution", "time in h", "");
		double duration = latest - earliest;
		
		double incr = duration/STEPS;
		double [] xs = new double[STEPS];
		double [] ys = new double[STEPS];
		
		double sqrt2PiSigmaSqr = Math.sqrt(2*Math.PI*sigma*sigma);
		
		for (int i = 1; i <= STEPS; i++) {
			
			double x = i*incr;
			
			double sqrDiff = (Math.log(x)-mu)*(Math.log(x)-mu);
			double y = 1/(x*sqrt2PiSigmaSqr)*Math.exp(-sqrDiff/(2*sigma*sigma));
			
			xs[i-1] = x+earliest;
			ys[i-1] = y;
		}
		chart.addSeries("model", xs, ys);
		setDrawable(chart.getChart());
	}
	
	
//	public void 
//	List<Tuple<Double, Double>> l = smoothData();
//	
//	double [] xs = new double[l.size()];
//	double [] ys = new double[l.size()];
//	int pos = 0;
//	for (Tuple<Double, Double> t : l) {
//
//		xs[pos] = t.getFirst();
//		ys[pos++] = t.getSecond();
//	}
//
//	chart.addSeries("model", xs, ys);
//	chart.saveAsPng(dir + "/flowFnd.png", 800, 400);

}
