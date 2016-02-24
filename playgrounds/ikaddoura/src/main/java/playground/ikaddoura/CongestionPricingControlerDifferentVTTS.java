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
package playground.ikaddoura;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;

import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author ikaddoura
 *
 */
public class CongestionPricingControlerDifferentVTTS {

	private static final Logger log = Logger.getLogger(CongestionPricingControlerDifferentVTTS.class);

	static String configFile;

	public static void main(String[] args) throws IOException {
		log.info("Starting simulation run with different VTTS...");
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);

		} else {
			configFile = "../../shared-svn/studies/ihab/test_siouxFalls/input/config.xml";
		}

		CongestionPricingControlerDifferentVTTS main = new CongestionPricingControlerDifferentVTTS();
		main.run();
	}

	private void run() {

		Controler controler = new Controler(configFile);

		TollHandler tollHandler = new TollHandler(controler.getScenario());
		final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});

		controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())));

		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();

	}
}

