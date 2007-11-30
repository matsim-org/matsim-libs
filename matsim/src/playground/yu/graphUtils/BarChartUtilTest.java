/* *********************************************************************** *
 * project: org.matsim.*
 * BarChartUtilTest.java
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

/**
 * @author ychen
 *
 */
public class BarChartUtilTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BarChartUtil bcu=new BarChartUtil("Ueberschrift","x-Achse","yAchse");
		bcu.addValue(bcu.dataset0, 25.0, "rowKeyA", "1");
		bcu.addValue(bcu.dataset0, 28.0, "rowKeyA", "2");
		bcu.addValue(bcu.dataset0, 15.0, "rowKeyA", "3");
		bcu.addValue(bcu.dataset0, 35.0, "rowKeyB", "1");
		bcu.addValue(bcu.dataset0, 19.0, "rowKeyB", "2");
		bcu.addValue(bcu.dataset0, 26.0, "rowKeyB", "3");
		bcu.addValue(bcu.dataset0, 21.0, "rowKeyA", "4");
		bcu.addValue(bcu.dataset0, 22.0, "rowKeyB", "4");
		bcu.saveAsPng("T:/Temp/bar.png", 800, 600);
		System.out.println("@done.");
	}
}
