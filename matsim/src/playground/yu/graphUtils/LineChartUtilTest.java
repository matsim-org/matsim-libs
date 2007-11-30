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


/**
 * @author ychen
 *
 */
public class LineChartUtilTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LineChartUtil lcu=new LineChartUtil("Ueberschrift","x-Achse","yAchse");
        lcu.addValue(lcu.dataset0, 25.0, "A", "1");
		lcu.addValue(lcu.dataset0, 28.0, "A", "2");
		lcu.addValue(lcu.dataset0, 15.0, "A", "3");
		lcu.addValue(lcu.dataset0, 35.0, "B", "1");
		lcu.addValue(lcu.dataset0, 19.0, "B", "2");
		lcu.addValue(lcu.dataset0, 26.0, "B", "3");
		lcu.addValue(lcu.dataset0, 21.0, "A", "4");
		lcu.addValue(lcu.dataset0, 22.0, "B", "4");
        lcu.saveAsPng("T:/Temp/line.png", 800, 600);
		System.out.println("@done.");
	}
}
