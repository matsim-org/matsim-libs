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

package org.matsim.core.utils.charts;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.ImageTitle;
import org.jfree.chart.title.Title;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.VerticalAlignment;
import org.matsim.core.gbl.MatsimResource;

/**
 * An abstract class to simplify the creation of JFreeCharts.
 *
 * @author mrieser
 * @author ychen
 */
public abstract class ChartUtil {

	protected final String chartTitle;
	protected final String xAxisLabel;
	protected final String yAxisLabel;
	protected JFreeChart chart = null;

	public ChartUtil(final String title, final String xAxisLabel, final String yAxisLabel) {
		this.chartTitle = title;
		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
	}

	public abstract JFreeChart getChart();

	/**
	 * Stores the chart as PNG image file.
	 *
	 * @param filename The filename of the resulting PNG file.
	 * @param width The width of the chart.
	 * @param height The height of the chart.
	 */
	public void saveAsPng(final String filename, final int width, final int height) {
		try {
			ChartUtils.saveChartAsPNG(new File(filename), getChart(), width, height, null, true, 9);
		} catch (IOException e) {
			e.printStackTrace();
		} catch ( Exception e ) {
			e.printStackTrace() ;
			// I just had an out of bounds error inside the method; don't know what that means but does not feel like a reason 
			// to not continue.  kai, apr'30
		}
	}

	/**
	 * Adds the MATSim Logo in the lower right corner of the chart.
	 */
	public void addMatsimLogo() {
		try {
			Image image = MatsimResource.getAsImage("matsim_logo_transparent_small.png");
			Title subtitle = new ImageTitle(image, RectangleEdge.BOTTOM, HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM);
			this.chart.addSubtitle(subtitle);
		} catch ( Exception e ) {
			e.printStackTrace() ;
			// I just had a resource-not-found error inside the method; don't know what that means but does not feel like a reason 
			// to not continue.  kai, apr'30
		}
	}

	/**
	 * Adds default formatting options for the charts, like a white background etc.
	 * Requires the member {@link #chart} to be set by the overriding class!
	 */
	protected void addDefaultFormatting() {
		this.chart.setBackgroundPaint(new Color(1.0f, 1.0f, 1.0f, 1.0f));
		this.chart.getLegend().setBorder(0.0, 0.0, 0.0, 0.0);
	}

}
