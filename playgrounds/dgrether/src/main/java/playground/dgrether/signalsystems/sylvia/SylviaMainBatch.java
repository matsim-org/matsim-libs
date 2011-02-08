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
		String config = "";
		config = baseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/cottbus_sylvia_config.xml";
		if (args != null && args.length == 2){
			baseDirectory = args[0];
			config = args[1];
			log.info("Running CottbusMainBatch with base directory: " + baseDirectory + " and config: " + config);
		}

		
		String fixedTimeSignals = baseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-lsa/signalControlCottbusT90_v2.0_jb_ba_removed.xml";
		String sylviaSignals = baseDirectory + "shared-svn/studies/dgrether/cottbus/sylvia/signal_control_sylvia.xml";
		
		String footballPlansBase = baseDirectory + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/planswithfb/output_plans_";
		

		for (int scale = 0; scale <= 100; scale = scale + 5){
			//fixed time control
			DgCottbusSylviaAnalysisControlerListener analysis = new DgCottbusSylviaAnalysisControlerListener();
			Controler controler = new Controler(config);
			String outputDirectory = controler.getConfig().controler().getOutputDirectory();
			controler.getConfig().controler().setOutputDirectory(outputDirectory + "fixed-time-control_scale_"+scale);
			controler.getConfig().plans().setInputFile( footballPlansBase + scale + ".xml.gz");
			controler.getConfig().signalSystems().setSignalControlFile(fixedTimeSignals);
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
			
			//sylvia control
			analysis = new DgCottbusSylviaAnalysisControlerListener();
			controler = new Controler(config);
			outputDirectory = controler.getConfig().controler().getOutputDirectory();
			controler.getConfig().controler().setOutputDirectory(outputDirectory + "sylvia-control_scale_"+scale);
			controler.getConfig().plans().setInputFile( footballPlansBase + scale + ".xml.gz");
			controler.getConfig().signalSystems().setSignalControlFile(sylviaSignals);
			controler.setSignalsControllerListenerFactory(new DgSylviaControlerListenerFactory());
			controler.addControlerListener(analysis);
			controler.setOverwriteFiles(true);
			controler.run();
		}
	}

}
