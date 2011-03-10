/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.scenarios;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import java.io.File;
import java.util.Random;

public class Create3TownsDemand {

	private final static Logger log = Logger.getLogger(Create3TownsDemand.class);
	private NetworkImpl network = null;
	private ScenarioImpl scenarioWriteOut = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private PopulationImpl staticPopulation = new PopulationImpl(scenarioWriteOut);
	private static int offset = 100000;
	private long seed = 109876L;
	public static String outputFolder="src/main/java/playground/anhorni/input/PLOC/3towns/";
	private static String path = "src/main/java/playground/anhorni/";

	private int populationSize = -1;
	
	//expenditure for home towns
	private double [] mu = {	0.0, 	0.0};
	private double [] sigma = {	0.0,	0.0};

	private ExpenditureAssigner expenditureAssigner = null;
	private ConfigReader configReader = new ConfigReader();
	
	private Random randomNumberGenerator = new Random(this.seed);

	public static void main(final String[] args) {
		String networkfilePath = path + "input/PLOC/3towns/network.xml";
		String facilitiesfilePath = path + "input/PLOC/3towns/facilities.xml";

		Create3TownsDemand plansCreator=new Create3TownsDemand();
		plansCreator.init(networkfilePath, facilitiesfilePath);

		plansCreator.run();
		plansCreator.createConfigs();
		log.info("Creation finished -----------------------------------------");
	}

	private void init(final String networkfilePath,
			final String facilitiesfilePath) {
		configReader.read();
		this.populationSize = configReader.getPopulationSize();
		this.mu = configReader.getMu();
		this.sigma = configReader.getSigma();
		this.expenditureAssigner = new ExpenditureAssigner(this.mu, this.sigma, path, this.seed);

		new MatsimNetworkReader(scenarioWriteOut).readFile(networkfilePath);
		new FacilitiesReaderMatsimV1(scenarioWriteOut).readFile(facilitiesfilePath);
	}
	
	  private void createConfigs() {
	    	Config config = new Config();
	    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
	    	matsimConfigReader.readFile(path + "/input/PLOC/3towns/config.xml");   	
	    	config.setParam("network", "inputNetworkFile", path + "input/PLOC/3towns/networks/" + 
	    			this.configReader.getPopulationSize() + "_network.xml");
	    	
	    	String outputPath = "";
	    	ConfigWriter configWriter = new ConfigWriter(config);
	    	for (int i = 0; i < configReader.getNumberOfRandomRuns(); i++) {

        		for (int j = 0; j < 5; j++) {
        			config.setParam("plans", "inputPlansFile", path + "input/PLOC/3towns/plans/run" + i + "/day" + j + "/plans.xml");
    	        	config.setParam("controler", "runId", "R" + Integer.toString(i) + "D" + j);
        			outputPath = path + "/output/PLOC/3towns/run" + i + "/day" + j + "/matsim";
        			new File(path + "/output/PLOC/3towns/run" + i + "/day" + j +"/matsim").mkdirs();
        			config.setParam("controler", "outputDirectory", outputPath);
        			String configPath = path + "/input/PLOC/3towns/configs/";
    	        	new File(configPath).mkdirs();
    	        	configWriter.write(configPath + "configR" + i + "D" + j + ".xml");
        		}	
	    	}	 	
	    }

	private void run() {		
		GeneratePopulation populationGenerator = new GeneratePopulation(this.randomNumberGenerator);
		populationGenerator.generatePopulation(populationSize, expenditureAssigner, staticPopulation, offset);
		
		MultiDaysGenerator multiDaysPlanGenerator = new MultiDaysGenerator(
				this.randomNumberGenerator.nextLong(), staticPopulation, scenarioWriteOut, network);
		
		for (int i = 0; i < configReader.getNumberOfRandomRuns(); i++) {			
			multiDaysPlanGenerator.generatePlans(i);
		}	
		new CreateNetworks().create(populationSize, false);
	}
}
