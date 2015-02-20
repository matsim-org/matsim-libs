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

/**
 * 
 */
package playground.ikaddoura.noise2;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.routing.TollDisutilityCalculatorFactory;

/**
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseOnlineControler {
	private static final Logger log = Logger.getLogger(NoiseOnlineControler.class);

	private static String configFile;
	private static double receiverPointGap;
	private static double scaleFactor;
	
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			
			configFile = args[0];
			log.info("Config file: " + configFile);
			
			receiverPointGap = Double.valueOf(args[1]);		
			log.info("Receiver point gap: " + receiverPointGap);
			
			scaleFactor = Double.valueOf(args[2]);		
			log.info("Population scale factor: " + scaleFactor);
			
		} else {
			
			configFile = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/noiseTestScenario/input/config.xml";
			receiverPointGap = 250.;
			scaleFactor = 10.;
		}
				
		NoiseOnlineControler noiseImmissionControler = new NoiseOnlineControler();
		noiseImmissionControler.run(configFile);
	}

	private void run(String configFile) {
		
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setReceiverPointGap(receiverPointGap);
		noiseParameters.setScaleFactor(scaleFactor);
		
		Controler controler = new Controler(configFile);

		NoiseContext noiseContext = new NoiseContext(controler.getScenario(), noiseParameters);
		
		TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(noiseContext);
		controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
				
		controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
		
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
}
