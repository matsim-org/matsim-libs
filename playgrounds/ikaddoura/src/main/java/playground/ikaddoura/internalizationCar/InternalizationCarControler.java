/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
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

/**
 * 
 */
package playground.ikaddoura.internalizationCar;


import java.io.IOException;
import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

/**
 * @author ikaddoura
 *
 */
public class InternalizationCarControler {
	
	private static final Logger log = Logger.getLogger(InternalizationCarControler.class);
	
	static String configFile;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
		} else {
			configFile = "/Users/Ihab/Desktop/car_internalization_input/config_internalization.xml";
//			configFile = "/Users/Ihab/Desktop/car_internalization_input/config_noInternalization.xml";
		}
		
		InternalizationCarControler main = new InternalizationCarControler();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.setOverwriteFiles(true);
		controler.addControlerListener(new InternalizationCarControlerListener( (ScenarioImpl) controler.getScenario()));
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.run();
		
	}
}
	
