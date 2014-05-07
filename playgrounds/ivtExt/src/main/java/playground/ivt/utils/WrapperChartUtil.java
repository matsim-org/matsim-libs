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
package playground.ivt.utils;

import org.apache.log4j.Logger;
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
	private static final Logger log =
		Logger.getLogger(WrapperChartUtil.class);


	private static final String TITLE = "";
	private static final String XLABEL = "";
	private static final String YLABEL = "";

	public WrapperChartUtil(final JFreeChart chart) {
		super(TITLE, XLABEL, YLABEL);
		this.chart = chart;
		try {
			this.addDefaultFormatting();
			//this.addMatsimLogo();
		}
		catch (Exception e) {
			// no default formating: not a big deal.
			log.warn( "Could not set the default formating for chart with title <<"+chart.getTitle().getText()
					+">>: got a "+e+" with message "+e.getMessage()
					//+" and stack trace "+e.getStackTrace()
					+". This is not fatal, but the plot may not look as expected." );
		}
	}

	@Override
	public JFreeChart getChart() {
		return this.chart;
	}
}

