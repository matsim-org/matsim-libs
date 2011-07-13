/* *********************************************************************** *
 * project: org.matsim.*
 * WrapperChartUtil.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.utils;

import org.jfree.chart.JFreeChart;
import org.matsim.core.utils.charts.ChartUtil;

/**
 * {@link ChartUtil} allowing to wrap any {@link JFreeChart} subtype.
 * This allows easy application of the standard formating on "unusual"
 * charts, without having to define a full ChartUtil.
 * <br>
 * Should only be used for "non durable" classes (like punctual analysis)
 *
 * @author thibautd
 */
public class WrapperChartUtil extends ChartUtil {

	private static final String TITLE = "";
	private static final String XLABEL = "";
	private static final String YLABEL = "";

	public WrapperChartUtil(final JFreeChart chart) {
		super(TITLE, XLABEL, YLABEL);
		this.chart = chart;
		//this.addMatsimLogo();
		this.addDefaultFormatting();
	}

	@Override
	public JFreeChart getChart() {
		return this.chart;
	}
}

