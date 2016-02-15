/* *********************************************************************** *
 * project: org.matsim.*
 * TaMain
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
package playground.dgrether.signalsystems.laemmer;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.laemmer.controler.LaemmerControlerListenerFactory;


/**
 * @author dgrether
 */
public class LaemmerMain {
  
	private static final Logger log = Logger.getLogger(LaemmerMain.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Running Laemmer main method...");
		String[] args2 = null;
		if (args == null || args.length == 0){
			log.info("No args given, running local config...");
			args2 = new String[1];
			args2[0] = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/cottbus_feb_fix/config.xml";
		}
		else {
			args2 = args;
		}

		
		Controler controler = new Controler(args2);
        //FIXME: Take care that the normal SignalsControllerListener is NOT added.
        controler.addControlerListener(new LaemmerControlerListenerFactory().createSignalsControllerListener());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();

	}

}
