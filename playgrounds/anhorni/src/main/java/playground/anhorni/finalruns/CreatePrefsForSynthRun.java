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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;


public class CreatePrefsForSynthRun {
	private final static Logger log = Logger.getLogger(CreatePrefsForSynthRun.class);
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private String outputFolder;
	private String plansFilePath;
	
	public static void main(final String[] args) {		
		CreatePrefsForSynthRun plansCreator = new CreatePrefsForSynthRun();
		
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
		ObjectAttributes prefs = new ObjectAttributes();
		int counter = 0;
		int nextMsg = 1;
		for (Person p : this.scenario.getPopulation().getPersons().values()) {	
			for (String type : ((PersonImpl)p).getDesires().getActivityDurations().keySet()) {
				double typicalDuration = ((PersonImpl)p).getDesires().getActivityDurations().get(type);
				prefs.putAttribute(p.getId().toString(), "typicalDuration_" + type, typicalDuration);
				prefs.putAttribute(p.getId().toString(), "minimalDuration_" + type, 0.5 * 3600.0);
				prefs.putAttribute(p.getId().toString(), "earliestEndTime_" + type, 0.0 * 3600.0);
				prefs.putAttribute(p.getId().toString(), "latestStartTime_" + type, 24.0 * 3600.0);
			}
			counter++;
			if (counter % nextMsg == 0) {
				nextMsg *= 2;
				log.info(" person # " + counter);
			}
		}
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(prefs);
		betaWriter.writeFile(outputFolder + "/prefs.xml");
	}
}
