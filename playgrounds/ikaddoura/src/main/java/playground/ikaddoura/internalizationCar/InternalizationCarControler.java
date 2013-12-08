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
	
	static String configFile1;
	static String configFile2;
	static String configFile3;
			
	public static void main(String[] args) throws IOException {
		
		configFile1 = args[0];
		configFile2 = args[1];
		configFile3 = args[2];
		
		InternalizationCarControler internalizationCarControler = new InternalizationCarControler();
		internalizationCarControler.runBaseCase(configFile1);
		internalizationCarControler.runInternalization(configFile2);
		internalizationCarControler.runBaseCase(configFile3);
	}
	
	private void runBaseCase(String configFile) {
		Controler controler = new Controler(configFile);
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.setOverwriteFiles(true);
		controler.run();
	}

	private void runInternalization(String configFile) {
		Controler controler = new Controler(configFile);

		TollHandler tollHandler = new TollHandler(controler.getScenario());
		TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
		controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
		controler.addControlerListener(new InternalizationCarControlerListener( (ScenarioImpl) controler.getScenario(), tollHandler ));
		
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.setOverwriteFiles(true);
		controler.run();
	}
}