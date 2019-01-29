/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.chart;

import java.awt.Dimension;

import javax.swing.*;

import org.jfree.chart.*;

public class ChartWindowUtils {
	public static void showFrame(JFreeChart chart) {
		ChartFrame frame = new ChartFrame(chart.getTitle().getText(), chart);
		SwingUtils.showWindow(frame, false);
	}

	public static void showFrame(JFreeChart chart, String title, int width, int height) {
		ChartFrame frame = new ChartFrame(title, chart);
		frame.setPreferredSize(new Dimension(width, height));
		SwingUtils.showWindow(frame, false);
	}

	public static void showDialog(JFreeChart chart, boolean modal) {
		JDialog dialog = newChartDialog(chart, chart.getTitle().getText(), modal);
		SwingUtils.showWindow(dialog, modal);
	}

	public static void showDialog(JFreeChart chart, String title, int width, int height, boolean modal) {
		JDialog dialog = newChartDialog(chart, title, modal);
		dialog.setPreferredSize(new Dimension(width, height));
		SwingUtils.showWindow(dialog, modal);
	}

	private static JDialog newChartDialog(JFreeChart chart, String title, boolean modal) {
		chart.setTitle(title);
		JDialog dialog = new JDialog();
		dialog.setTitle(title);
		dialog.setContentPane(new ChartPanel(chart));
		dialog.setModal(modal);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		return dialog;
	}
}
