/* *********************************************************************** *
 * project: org.matsim.*
 * Z_Launcher.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.mmoyo.cadyts_integration;

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.utils.misc.ConfigUtils;

public class Z_Launcher {

	public static void main(final String[] args) {
		String configFile = null ;
		if (args.length==1){
			configFile = args[0];
		}else{
			configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/100plans_bestValues_config.xml";
		}

		Config config = null;
		try {
			config = ConfigUtils.loadConfig(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
	
		ControlerListener ptBseUCControlerListener = new NewPtBseUCControlerListener();
		controler.addControlerListener(ptBseUCControlerListener);
		
		controler.run();
	}
}
