/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.finalruns;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;


public class AddBetasForSynthRun {
	private final static Logger log = Logger.getLogger(AddBetasForSynthRun.class);
	private Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String outputFolder;
	private String plansFilePath;
	
	public static void main(final String[] args) {		
		AddBetasForSynthRun plansCreator = new AddBetasForSynthRun();
		
		 String plansFilePath = args[0]; 
		 String outputFolder = args[1];
		
		plansCreator.run(plansFilePath, outputFolder);			
		log.info("Adaptation finished -----------------------------------------");
	}
		
	private void init() {		
		MatsimPopulationReader populationReader = new MatsimPopulationReader(this.scenario);
		populationReader.readFile(plansFilePath);
	}
	
	public void run(final String plansFilePath, final String outputFolder) {
		this.plansFilePath = plansFilePath;
		this.outputFolder = outputFolder;	
		this.init();		
		this.writeBetas();	
	}
	
	private void writeBetas() {
		ObjectAttributes betas = new ObjectAttributes();
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {	
			betas.putAttribute(p.getId().toString(), "size", 1.0);
			betas.putAttribute(p.getId().toString(), "price", -1.0);
			betas.putAttribute(p.getId().toString(), "tauagglo", 1.0);
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(betas);
		betaWriter.writeFile(outputFolder + "/betas.xml");
	}
}
