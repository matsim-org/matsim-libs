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
		bcu.addValue(25.0, "rowKeyA", "1");
		bcu.addValue(28.0, "rowKeyA", "2");
		bcu.addValue(15.0, "rowKeyA", "3");
		bcu.addValue(35.0, "rowKeyB", "1");
		bcu.addValue(19.0, "rowKeyB", "2");
		bcu.addValue(26.0, "rowKeyB", "3");
		bcu.addValue(21.0, "rowKeyA", "4");
		bcu.addValue(22.0, "rowKeyB", "4");
//		for(int i=0;i<100;i++){
//			bcu.addValue(Math.random(),"0",Integer.toString(i));
//		}
		bcu.saveAsPng("T:/Temp/bar.png", 800, 600);
		System.out.println("@done.");
	}
}
