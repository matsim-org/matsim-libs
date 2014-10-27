/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise2;

import java.io.IOException;

import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

/**
 * 1) Calculate noise emissions for each link and time interval.
 * 2) Calculate noise immissions for each receiver point and save the contribution of each link to the receiver point's immission level.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseImmissionControler {
	
	private static String configFile;
	public static void main(String[] args) throws IOException {
		
		configFile = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/noiseTestScenario/input/config.xml";
		
		NoiseImmissionControler noiseImmissionControler = new NoiseImmissionControler();
		noiseImmissionControler.run(configFile);
	}

	private void run(String configFile) {
		
		Controler controler = new Controler(configFile);
		
		controler.addControlerListener(new NoiseControlerListener());
		
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());	
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
}
