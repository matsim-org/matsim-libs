/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ciarif.flexibletransports.preprocess.membership;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesReaderMatsimV1;

public class MembershipMain {
	
		private final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		private final Population plans = scenario.getPopulation();
		private final ActivityFacilities facilities = scenario.getActivityFacilities();
		private final Network network = scenario.getNetwork();
		private String plansfilePath;
		private final String outpath = "../../matsim/output/preprocess/"; // @burgess
		//private final String outpath = "/data/matsim/ciarif/output/preprocess/"; //@satawal
		private String facilitiesfilePath;
		private String networkfilePath;
		
	
		private final static Logger log = Logger.getLogger(MembershipMain.class);

		public static void main(String [] args) {
			Gbl.startMeasurement();
			final MembershipMain membershipMain = new MembershipMain();
			membershipMain.run(args [0]);
			Gbl.printElapsedTime();
		}

		public void run(String inputFile) {
			this.init(inputFile);
			MembershipAssigner membershipAssigner = new MembershipAssigner(this.scenario);
			membershipAssigner.run();
			this.writePlans();
		}

		private void readInputFile(final String pathsFile) {
			try {
				FileReader fileReader = new FileReader(pathsFile);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				
				this.networkfilePath = bufferedReader.readLine();
				this.facilitiesfilePath = bufferedReader.readLine();
				this.plansfilePath = bufferedReader.readLine();
				bufferedReader.close();
				fileReader.close();

			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void init(String inputFile) {
			String pathsFile = inputFile;
			this.readInputFile(pathsFile);
			
			log.info("reading the facilities ...");
			new FacilitiesReaderMatsimV1(this.scenario).readFile(facilitiesfilePath);

			log.info("reading the network ...");
			new MatsimNetworkReader(this.scenario).readFile(networkfilePath);
			
			log.info("  reading file " + plansfilePath);
			final PopulationReader plansReader = new MatsimPopulationReader(this.scenario);
			plansReader.readFile(plansfilePath);
		}

		private void writePlans() {
			
			new PopulationWriter(this.plans, this.network).write(this.outpath + "plansCarSharing.xml");
		}
}
