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
public class BarChartUtilTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BarChartUtil bcu=new BarChartUtil("Ueberschrift","x-Achse","yAchse");
//        lcu.addValue(25.0, "A", "1");
//		lcu.addValue(28.0, "A", "2");
//		lcu.addValue(15.0, "A", "3");
//		lcu.addValue(35.0, "B", "1");
//		lcu.addValue(19.0, "B", "2");
//		lcu.addValue(26.0, "B", "3");
//		lcu.addValue(21.0, "A", "4");
//		lcu.addValue(22.0, "B", "4");
		for(int i=0;i<100;i++){
			bcu.addValue(Math.random(),"0",Integer.toString(i));
		}
        bcu.saveAsPng("T:/Temp/bar1.png", 800, 600);
		System.out.println("1@done.");
		bcu=new BarChartUtil("Ueberschrift","x-Achse","yAchse");
		for(int i=0;i<100;i++){
			bcu.addValue(Math.random(),Integer.toString(i));
		}
		bcu.saveAsPng("T:/Temp/bar2.png",800,600);
		System.out.println("2@done.");
	}
}
