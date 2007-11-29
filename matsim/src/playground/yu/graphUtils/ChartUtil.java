/* *********************************************************************** *
 * project: org.matsim.*
 * ChartUtil.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.graphUtils;

import org.jfree.chart.JFreeChart;

/**
 * @author yu
 * 
 */
public abstract class ChartUtil {
	protected JFreeChart chart_;

	/**
	 * 
	 */
	public ChartUtil() {
		super();
		// TODO Auto-generated constructor stub
	}

	public abstract JFreeChart createChart(String title, String xAxisLabel,
			String yAxisLabel);
	public abstract void addData(Object[] values);
}
