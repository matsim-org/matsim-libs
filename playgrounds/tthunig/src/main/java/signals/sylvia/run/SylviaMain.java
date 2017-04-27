/* *********************************************************************** *
 * project: org.matsim.*
 * SylviaMain
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
package signals.sylvia.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import signals.CombinedSignalsModule;


/**
 * @author dgrether
 *
 */
public class SylviaMain {

	private static final Logger log = Logger.getLogger(SylviaMain.class);
	
	public static void main(String[] args) {
		log.info("Running SylviaMain...");
		String[] args2 = null;
		if (args == null || args.length == 0){
			throw new RuntimeException("No arguments given, expecting path to config!");
		}
		else {
			args2 = args;
		}

//		DgCottbusSylviaAnalysisControlerListener analysis = new DgCottbusSylviaAnalysisControlerListener();
		
		Controler controler = new Controler(args2);
		controler.addOverridingModule(new CombinedSignalsModule());
		//		controler.addControlerListener(analysis);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
	}

}
