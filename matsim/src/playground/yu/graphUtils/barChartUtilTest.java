/* *********************************************************************** *
 * project: org.matsim.*
 * barCharUtilTest.java
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

import org.jfree.data.category.DefaultCategoryDataset;

/**
 * @author ychen
 *
 */
public class barChartUtilTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BarChartUtil bcu=new BarChartUtil("Ueberschrift","x-Achse","yAchse");
//		DefaultCategoryDataset newDataSet=new DefaultCategoryDataset();
//		bcu.setDataSets(new DefaultCategoryDataset[]{newDataSet});
		bcu.addValue(bcu.dataset0, 25.0, "rowKeyA", "columnKey1");
		bcu.addValue(bcu.dataset0, 28.0, "rowKeyA", "columnKey2");
		bcu.addValue(bcu.dataset0, 15.0, "rowKeyA", "columnKey3");
		bcu.addValue(bcu.dataset0, 35.0, "rowKeyB", "columnKey1");
		bcu.addValue(bcu.dataset0, 19.0, "rowKeyB", "columnKey2");
		bcu.addValue(bcu.dataset0, 26.0, "rowKeyB", "columnKey3");
//		bcu.addValue(newDataSet, 24.0, "rowKeyA", "columnKey1");
//		bcu.addValue(newDataSet, 29.0, "rowKeyA", "columnKey2");
//		bcu.addValue(newDataSet, 12.0, "rowKeyA", "columnKey3");
//		bcu.addValue(newDataSet, 32.0, "rowKeyB", "columnKey1");
//		bcu.addValue(newDataSet, 14.0, "rowKeyB", "columnKey2");
//		bcu.addValue(newDataSet, 20.0, "rowKeyB", "columnKey3");
		bcu.saveAsPng("T:/Temp/1.png", 800, 600);
	}
}
