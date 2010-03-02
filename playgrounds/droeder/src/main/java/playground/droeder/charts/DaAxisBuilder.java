/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.charts;

import java.awt.Font;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;

/**
 * @author droeder
 *
 */
public class DaAxisBuilder {
	
	
	private Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);
	private Font axisFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
	
	
	public CategoryAxis createCategoryAxis(String xLabel) {
		CategoryAxis categoryAxis = new CategoryAxis(xLabel);
		categoryAxis.setCategoryMargin(0.07); // percentage of space between categories
		categoryAxis.setLowerMargin(0.03); // percentage of space before first bar
		categoryAxis.setUpperMargin(0.03); // percentage of space after last bar
		categoryAxis.setLabelFont(labelFont);
		categoryAxis.setTickLabelFont(axisFont);
		return categoryAxis;
	}
	
	public ValueAxis createValueAxis(String yLabel) {
		ValueAxis valueAxis = new NumberAxis(yLabel);
		valueAxis.setLabelFont(labelFont);
		valueAxis.setTickLabelFont(axisFont);
		valueAxis.setUpperMargin(100);
		return valueAxis;
	}
	public ValueAxis createValueAxis(String yLabel, double yMax) {
		ValueAxis valueAxis = new NumberAxis(yLabel);
		valueAxis.setLabelFont(labelFont);
		valueAxis.setTickLabelFont(axisFont);
		valueAxis.setUpperBound(yMax);
		return valueAxis;
	}

	
	public Font getLabelFont() {
		return labelFont;
	}
	
	
	public Font getAxisFont() {
		return axisFont;
	}

}
