/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler4.java
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

package playground.yu.test;

import java.io.IOException;

import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;

import playground.yu.analysis.PtCheck2;

public class PtCheckTest {

	public static void main(final String[] args) {
		final String netFilename = "./test/yu/test/input/network.xml";
		final String plansFilename = "./test/yu/test/input/10pctZrhCarPt100.plans.xml.gz";
		final String ptcheckFilename = "./test/yu/test/output/ptCheck100.10pctZrhCarPt.txt";
		Gbl
				.createConfig(new String[] { "./test/yu/test/configPtcheckTest.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new PopulationImpl();
		try {
			PtCheck2 pc = new PtCheck2(ptcheckFilename);

			population.addAlgorithm(pc);
			new MatsimPopulationReader(population, network)
					.readFile(plansFilename);
			population.runAlgorithms();

			pc.write(100);
			pc.writeEnd();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("-->Done!!");
	}

}
