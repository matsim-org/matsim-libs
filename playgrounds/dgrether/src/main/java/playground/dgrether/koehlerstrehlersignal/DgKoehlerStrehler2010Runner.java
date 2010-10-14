/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010Runner
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal;

import org.matsim.core.controler.Controler;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgKoehlerStrehler2010Runner {

	public static final String defaultConfigFile = DgPaths.STUDIESDG + "koehlerStrehler2010/config.xml";
	
	private String configFile = defaultConfigFile;
	
	
	private void runFromConfig() {
		Controler controler = new Controler(configFile);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	
	public static void main(String[] args) {
		new DgKoehlerStrehler2010Runner().runFromConfig();
	}


}
