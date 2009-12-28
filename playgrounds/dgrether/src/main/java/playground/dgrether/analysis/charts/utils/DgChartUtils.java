/* *********************************************************************** *
 * project: org.matsim.*
 * DgChartUtils
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
package playground.dgrether.analysis.charts.utils;

import java.util.List;

import org.matsim.core.utils.collections.Tuple;


/**
 * @author dgrether
 *
 */
public class DgChartUtils {

	
	public static Tuple<double[], double[]> createArray(List<Tuple<Double, Double>> list) {
		double[] xvalues = new double[list.size()];
		double[] yvalues = new double[list.size()];
		int i = 0;
		for (Tuple<Double, Double> t : list) {
			xvalues[i] = t.getFirst();
			yvalues[i] = t.getSecond();
			i++;
		}
		return new Tuple<double[], double[]>(xvalues, yvalues);
	}
	
	
	
}
