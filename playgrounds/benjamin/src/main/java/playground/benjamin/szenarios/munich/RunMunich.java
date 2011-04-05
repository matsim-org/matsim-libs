/* *********************************************************************** *
 * project: org.matsim.*
 * RunMunich.java
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
package playground.benjamin.szenarios.munich;

import java.io.IOException;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.gis.ShapeFileReader;

/**
 * @author benjamin
 *
 */
public class RunMunich {
	
	static String configFile = "../../detailedEval/testRuns/input/config.xml";
	static String zone30Shape = "../../detailedEval/policies/mobilTUM/zone30.shp";
	static boolean considerZone30 = true;
	

	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader confReader = new MatsimConfigReader(config);
		confReader.readFile(configFile);
		Controler controler = new Controler(config);
		
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(true);
		
		if(considerZone30){
			Set<Feature> featuresInZone30 = readShape(zone30Shape);
			controler.addControlerListener(new SetLinkAttributesControlerListener (featuresInZone30));
		}
		controler.run();
	}


	private static Set<Feature> readShape(String shapeFile) {
		final Set<Feature> featuresInZone30;
		try {
			featuresInZone30 = new ShapeFileReader().readFileAndInitialize(shapeFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return featuresInZone30;
	}
}