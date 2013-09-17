/* *********************************************************************** *
 * project: org.matsim.*
 * TestMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether;

import java.text.DecimalFormat;
import java.util.regex.Matcher;




/**
 * @author dgrether
 *
 */
public class TestMain {

	public static void testVarArgs(String...strings) {
		for (String s : strings)
			System.out.println(s);
	}
	
	public static void testVarArgsMain(String[] args) {
		testVarArgs("hallo", "ihr", "penner");
		testVarArgs("hallo");
	}
	
	public static void testNumberFormat(){
		double d = 312380980328479.5;
		System.out.println(Double.toString(d));
		DecimalFormat f = new DecimalFormat("#.#");
		System.out.println(f.format(d));
		f = new DecimalFormat("#.00");
		System.out.println(f.format(d));
		f = new DecimalFormat("#");
		System.out.println(f.format(d));
	
	}
	
	private static void testReplaceAll() {
		String test = "aasdf_asdf";
		String test2 = test.replaceAll("_", Matcher.quoteReplacement(" "));
		System.out.println(test2);
		test = "asladfj % afddasjal";
		test2 = test.replaceAll("%", Matcher.quoteReplacement("\\%"));
		System.out.println(test2);
	}
	
	private static void testRound(){
		double d = 0.5;
		System.out.println(Math.round(d));
		d = -0.5;
		System.out.println(Math.round(d));
	}

	public static void main(String[] args){
		String config = "/media/data/work/matsim/examples/stephan_rath/testcase3/config.xml";
//		Controler c = new Controler(config);
//		c.run();
//		OTFVis.playConfig(config);
//		testNumberFormat();
//		testReplaceAll();
		testRound();
		
	}


}
