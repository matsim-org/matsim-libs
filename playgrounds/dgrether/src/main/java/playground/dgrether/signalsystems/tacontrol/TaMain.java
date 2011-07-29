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
package playground.dgrether.signalsystems.tacontrol;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import playground.dgrether.DgPaths;
import playground.dgrether.signalsystems.tacontrol.controler.DgTaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class TaMain {
  
	private static final Logger log = Logger.getLogger(TaMain.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log.info("Running SylviaMain...");
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
		controler.setSignalsControllerListenerFactory(new DgTaControlerListenerFactory());
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
