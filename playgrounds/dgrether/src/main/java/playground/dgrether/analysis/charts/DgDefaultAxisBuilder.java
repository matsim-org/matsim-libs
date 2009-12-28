/* *********************************************************************** *
 * project: org.matsim.*
 * DgDefaultAxisFactory
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
package playground.dgrether.analysis.charts;

import java.awt.Font;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;


/**
 * @author dgrether
 *
 */
public class DgDefaultAxisBuilder implements DgAxisBuilder {
	
	
	private Font labelFont = new Font("Helvetica", Font.BOLD, 18);
	private Font axisFont = new Font("Helvetica", Font.BOLD, 14);
	
	
	public CategoryAxis createCategoryAxis(String xLabel) {
		CategoryAxis categoryAxis = new CategoryAxis(xLabel);
		categoryAxis.setCategoryMargin(0.05); // percentage of space between categories
		categoryAxis.setLowerMargin(0.01); // percentage of space before first bar
		categoryAxis.setUpperMargin(0.01); // percentage of space after last bar
		categoryAxis.setLabelFont(labelFont);
		categoryAxis.setTickLabelFont(axisFont);
		return categoryAxis;
	}
	
	public ValueAxis createValueAxis(String yLabel) {
		ValueAxis valueAxis = new NumberAxis(yLabel);
		valueAxis.setLabelFont(labelFont);
		valueAxis.setTickLabelFont(axisFont);
		return valueAxis;
	}

	
	public Font getLabelFont() {
		return labelFont;
	}

	
	public Font getAxisFont() {
		return axisFont;
	}

}
