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

package org.matsim.utils.charts;

import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.File;
import java.io.IOException;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.ImageTitle;
import org.jfree.chart.title.Title;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.VerticalAlignment;
import org.matsim.gbl.MatsimResource;

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

	protected abstract JFreeChart getChart();

	/**
	 * Stores the chart as PNG image file.
	 *
	 * @param filename The filename of the resulting PNG file.
	 * @param width The width of the chart.
	 * @param height The height of the chart.
	 */
	public void saveAsPng(final String filename, final int width, final int height) {
		try {
			ChartUtilities.saveChartAsPNG(new File(filename), getChart(), width, height, null, true, 9);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Adds the MATSim Logo in the lower right corner of the chart.
	 */
	public void addMatsimLogo() {
		Image image = MatsimResource.getAsImage("matsim_logo_transparent_small.png");
		// make sure the image is really loaded. See JavaDoc for ImageTitle.
    MediaTracker mediaTracker = new MediaTracker(new Container());
    mediaTracker.addImage(image, 0);
    try {
			mediaTracker.waitForID(0);
			Title subtitle = new ImageTitle(image, RectangleEdge.BOTTOM, HorizontalAlignment.RIGHT, VerticalAlignment.BOTTOM);
			this.chart.addSubtitle(subtitle);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
