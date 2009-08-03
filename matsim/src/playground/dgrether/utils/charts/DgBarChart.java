/* *********************************************************************** *
 * project: org.matsim.*
 * DgBarChart
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

import org.jfree.chart.JFreeChart;
import org.matsim.core.utils.charts.ChartUtil;


/**
 * @author dgrether
 *
 */
public class DgBarChart extends ChartUtil {

	public DgBarChart(String title, String xAxisLabel, String yAxisLabel) {
		super(title, xAxisLabel, yAxisLabel);
	}

	@Override
	protected JFreeChart getChart() {
		return null;
	}

}
