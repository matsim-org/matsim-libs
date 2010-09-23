/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleRunner.java
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

package playground.msieg;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import playground.msieg.cmcf.BestFitTimeRouter;
import playground.msieg.cmcf.CMCFDemandWriter;
import playground.msieg.cmcf.CMCFNetworkWriter;
import playground.msieg.cmcf.CMCFRouter;
import playground.msieg.cmcf.RandomPlansGenerator;

public class SimpleRunner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		test3();
	}

	public static void test1(){
		CMCFNetworkWriter.main(
				new String[] {"examples/siouxfalls/network.xml", "examples/test/sioux/netCMCF.xml"});
		RandomPlansGenerator.main(
				new String[] {"examples/siouxfalls/network.xml", "1000"});
	}

	public static void test2() throws IOException{
		CMCFDemandWriter cdw = new CMCFDemandWriter("examples/test/sioux/config.xml");
		try {
			Writer out =  new FileWriter("examples/test/sioux/plansCMCF.xml");
			cdw.setInputNetwork("examples/test/sioux/network.xml");
			cdw.readFile();
			cdw.convert(out, 6);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void test3(){
		CMCFRouter btr = new BestFitTimeRouter(
				"examples/test/sioux/network.xml",
				"examples/test/sioux/randPlans1000_1232377612644.xml",
				"examples/test/sioux/res1000.cmcf",
				6);
		btr.loadEverything();
		btr.route();
		btr.writePlans("examples/test/sioux/routedPlans1000_1232377612644.xml");
	}
}

