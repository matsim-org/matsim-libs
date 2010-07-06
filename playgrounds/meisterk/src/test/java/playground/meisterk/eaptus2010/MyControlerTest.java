/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.eaptus2010;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.testcases.MatsimTestCase;

public class MyControlerTest extends MatsimTestCase {

	@Test
	public void testRun() {

		String[] args = new String[]{this.getInputDirectory() + "config_equil_changeExpBeta0.8.xml"};

		MyControler myControler = new MyControler(args);
		myControler.getConfig().controler().setOutputDirectory(this.getOutputDirectory());
		myControler.setCreateGraphs(false);
		myControler.setMovingAverageSkipAndPeriod(3);
		myControler.run();

		File file = new File(this.getOutputDirectory() + "changeProbStats.txt");

		try {
			BufferedReader in = new BufferedReader(new FileReader(file));

			String line = null;
			int iterationLineCounter = 0;

			while((line = in.readLine()) != null) {

				if (line.startsWith("#")) {
					// do nothing in the case of a commented line
				} else {

					String[] tokens = StringUtils.explode(line, '\t');
					int iterationNumber = Integer.parseInt(tokens[0]);
					double changeQuote = Double.parseDouble(tokens[1]);
					double movingAverage = Double.parseDouble(tokens[2]);
					assertEquals(iterationLineCounter, iterationNumber);
					if (iterationLineCounter == 10) {
						assertEquals(0.006313131313131313, changeQuote, MatsimTestCase.EPSILON);
						assertEquals(0.0079729152092876, movingAverage, MatsimTestCase.EPSILON);
					}

					iterationLineCounter++;

				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
