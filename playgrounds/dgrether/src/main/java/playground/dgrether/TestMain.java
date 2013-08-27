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
	}
	

	public static void main(String[] args){
		String config = "/media/data/work/matsim/examples/stephan_rath/testcase3/config.xml";
//		Controler c = new Controler(config);
//		c.run();
//		OTFVis.playConfig(config);
		testNumberFormat();
	}

}
