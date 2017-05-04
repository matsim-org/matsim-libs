/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
* @author ikaddoura
*/

public class BerlinControler2 {

	private static final Logger log = Logger.getLogger(BerlinControler2.class);
	
	static String configFile;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
		} else {
			configFile = "../../../runs-svn/cne/berlin-dz-1pct/input/config_be_117j_baseCaseCtd_detailedNetwork.xml";
//			configFile = "../../../runs-svn/cne/berlin-dz-1pct/input/config_be_117j_baseCaseCtd.xml";
		}
		
		BerlinControler2 main = new BerlinControler2();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		
		controler.run();
	}
}

