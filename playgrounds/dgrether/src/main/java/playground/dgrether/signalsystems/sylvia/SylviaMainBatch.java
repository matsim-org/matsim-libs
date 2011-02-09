/* *********************************************************************** *
 * project: org.matsim.*
 * SylviaMainBatch
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
package playground.dgrether.signalsystems.sylvia;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class SylviaMainBatch {

	
	private static final Logger log = Logger.getLogger(SylviaMainBatch.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String baseDirectory = DgPaths.REPOS;
		String configFilename = "";
		configFilename = baseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/cottbus_sylvia_config.xml";
		if (args != null && args.length == 2){
			baseDirectory = args[0];
			configFilename = args[1];
			log.info("Running CottbusMainBatch with base directory: " + baseDirectory + " and config: " + configFilename);
		}

		
		String fixedTimeSignals = baseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-lsa/signalControlCottbusT90_v2.0_jb_ba_removed.xml";
		String sylviaSignals = baseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/signal_control_sylvia.xml";
		
		String footballPlansBase = baseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/planswithfb/output_plans_";
		
		Config baseConfig = new ScenarioImpl().getConfig();
		MatsimConfigReader confReader = new MatsimConfigReader(baseConfig);
		confReader.readFile(configFilename);

		for (int scale = 0; scale <= 100; scale = scale + 5){
			//fixed time control
			DgCottbusSylviaAnalysisControlerListener analysis = new DgCottbusSylviaAnalysisControlerListener();
			String outputDirectory = baseConfig.controler().getOutputDirectory();
			baseConfig.controler().setOutputDirectory(outputDirectory + "fixed-time-control_scale_"+scale);
			baseConfig.plans().setInputFile( footballPlansBase + scale + ".xml.gz");
			baseConfig.signalSystems().setSignalControlFile(fixedTimeSignals);
			Controler controler = new Controler(baseConfig);
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
			
			//sylvia control
			analysis = new DgCottbusSylviaAnalysisControlerListener();
			outputDirectory = controler.getConfig().controler().getOutputDirectory();
			baseConfig.controler().setOutputDirectory(outputDirectory + "sylvia-control_scale_"+scale);
			baseConfig.plans().setInputFile( footballPlansBase + scale + ".xml.gz");
			baseConfig.signalSystems().setSignalControlFile(sylviaSignals);

			controler = new Controler(baseConfig);
			controler.setSignalsControllerListenerFactory(new DgSylviaControlerListenerFactory());
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
		}
	}

}
