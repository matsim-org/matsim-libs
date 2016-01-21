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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

/**
 * @author ikaddoura
 *
 */
public class IKControler {
	
	private static final Logger log = Logger.getLogger(IKControler.class);
	
	static String configFile;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/cottbus/input/config.xml";
		}
		
		IKControler main = new IKControler();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		//		services.addControlerListener(new IKControlerListener(services.getScenario()));
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
	}
}
	
