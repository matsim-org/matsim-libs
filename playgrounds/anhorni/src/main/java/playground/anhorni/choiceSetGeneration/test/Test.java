/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.choiceSetGeneration.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

	public class Test {

		private Controler controler = null;
		private final static Logger log = Logger.getLogger(Test.class);
		private String matsimRunConfigFile = null;

		public static void main(String[] args) {
			// for the moment hard-coding
			String inputFile = "./input/input.txt";
			Test generator = new Test();
			generator.readInputFile(inputFile);
			generator.run();
		}

		private void readInputFile(String inputFile) {
			try {
				FileReader fileReader = new FileReader(inputFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);

				this.matsimRunConfigFile = bufferedReader.readLine();
				log.info("MATSim config file: " + this.matsimRunConfigFile);

				bufferedReader.close();
				fileReader.close();

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public void run() {
			this.controler = new Controler(this.matsimRunConfigFile);
			ExtractChoiceSetsRoutingTest listenerCar = new ExtractChoiceSetsRoutingTest(this.controler);
			controler.addControlerListener(listenerCar);
			controler.run();
		}
}
