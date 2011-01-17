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

package playground.anhorni.PLOC.create;

import java.io.File;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;

import playground.anhorni.LEGO.miniscenario.create.CreateScenario;
import playground.anhorni.PLOC.PLOCConfigReader;


public class CreateScenarios {

	private final static Logger log = Logger.getLogger(CreateScenarios.class);	
	private PLOCConfigReader plocConfigReader = new PLOCConfigReader();
	private static String pathPre = "src/main/java/playground/anhorni/";
		
	public static void main(final String[] args) {
		CreateScenarios multipleScenariosCreator = new CreateScenarios();		
		multipleScenariosCreator.run();		
		
		log.info("Scenarios creation finished \n ----------------------------------------------------");
	}
	

	private void run() {
		Random rnd = new Random();
		
		plocConfigReader.read();
		log.info(plocConfigReader.getNumberOfRandomRuns());
		
		for (int i = 0; i < plocConfigReader.getNumberOfRandomRuns(); i++) {
			CreateScenario scenarioCreator = new CreateScenario();
			
			String path = pathPre + "input/PLOC/run" + i + "/";
			new File(path).mkdir();
			
			scenarioCreator.setOutPath(path);
			scenarioCreator.setSeed(rnd.nextLong());
			scenarioCreator.run();
		}
		this.createConfigs();
	}
	
	private void createConfigs() {
    	Config config = new Config();
    	MatsimConfigReader configReader = new MatsimConfigReader(config);
    	configReader.readFile(pathPre + "input/PLOC/config.xml");   	
    	    	
    	for (int i = 0; i < plocConfigReader.getNumberOfRandomRuns(); i++) {
    		config.setParam("plans", "inputPlansFile", pathPre + "input/PLOC/run " + i + "/plans.xml");
        	config.setParam("controler", "runId", "run" + String.valueOf(i));
        	String outputPath = pathPre + "output/PLOC/run" + i + "/";
        	new File(outputPath).mkdir();
        	config.setParam("controler", "outputDirectory", outputPath);
        	ConfigWriter configWriter = new ConfigWriter(config);
        	configWriter.write(pathPre + "input/PLOC/run" + i + "/config.xml");
    	}  	
    }
}
