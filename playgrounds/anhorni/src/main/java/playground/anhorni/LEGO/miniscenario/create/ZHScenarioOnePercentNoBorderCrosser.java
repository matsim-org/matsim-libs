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

package playground.anhorni.LEGO.miniscenario.create;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.anhorni.utils.PlansRemoverById;
import playground.anhorni.utils.PlansSampler;


public class ZHScenarioOnePercentNoBorderCrosser {
	private final static Logger log = Logger.getLogger(ZHScenarioOnePercentNoBorderCrosser.class);
	private ScenarioImpl scenario;
	private String outputFolder;
			
	public static void main(final String[] args) {		
		ZHScenarioOnePercentNoBorderCrosser plansCreator = new ZHScenarioOnePercentNoBorderCrosser();		
		plansCreator.run(args[0], Double.parseDouble(args[1]));			
		log.info("Creation finished -----------------------------------------");
	}
		
	private void init(final String plansFilePath, final String networkFilePath) {
		new MatsimNetworkReader(scenario).readFile(networkFilePath);		
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}

	public void run(String configFile, double sampleFraction) {
		Config config = new Config();
    	MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
    	matsimConfigReader.readFile(configFile);
		this.scenario  = (ScenarioImpl) ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		this.init(config.getModule("plans").getValue("inputPlansFile"), 
				config.getModule("network").getValue("inputNetworkFile"));
		
		this.outputFolder = config.getModule("controler").getValue("outputDirectory");
		this.cleanPlans();
		this.samplePlans(sampleFraction);		
		this.write();
	}
	
	private void cleanPlans() {
		PlansRemoverById remover = new PlansRemoverById();
		Population cleanedPop = remover.remove(this.scenario.getPopulation(), new IdImpl(1000000000));
		this.scenario.setPopulation(cleanedPop);
	}
	
	private void samplePlans(double sampleFraction) {
		PlansSampler sampler = new PlansSampler();
		Population sampledPop = sampler.sample(this.scenario.getPopulation(), sampleFraction);
		this.scenario.setPopulation(sampledPop);
	}
	
	private void write() {
		new File(this.outputFolder).mkdirs();
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(this.outputFolder + "plans.xml.gz");
	}
}
