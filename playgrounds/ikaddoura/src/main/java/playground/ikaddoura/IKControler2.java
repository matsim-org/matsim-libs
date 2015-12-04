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
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

/**
 * @author ikaddoura
 *
 */
public class IKControler2 {
		
	static String configFile;
	static String netFile;
	static String popFile;
	static String outputDirectory;
			
	public static void main(String[] args) throws IOException {
				
		if (args.length > 0) {
			throw new RuntimeException("Not implemented. Aborting...");
			
		} else {
			
			configFile = "../../../public-svn/matsim/scenarios/countries/de/berlin/car-traffic-only-1pct-2014-08-01/config_be_1pct.xml";
			netFile = "../../../public-svn/matsim/scenarios/countries/de/berlin/car-traffic-only-1pct-2014-08-01/network.xml.gz";
			popFile = "../../../public-svn/matsim/scenarios/countries/de/berlin/car-traffic-only-1pct-2014-08-01/run_160.150.plans_selected.xml.gz";
			outputDirectory = "../../../runs-svn/berlin-1pct/";
		}
		
		IKControler2 main = new IKControler2();
		main.run();
	}
	
	private void run() {
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(netFile);
		config.plans().setInputFile(popFile);
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(outputDirectory);
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.run();
	}
}
	
