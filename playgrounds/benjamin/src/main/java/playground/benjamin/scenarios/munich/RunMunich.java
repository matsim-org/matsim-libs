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
package playground.benjamin.scenarios.munich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

/**
 * @author benjamin
 *
 */
public class RunMunich {
	
	static String configFile;
	static String zone30Path = "../../detailedEval/policies/mobilTUM/";
	static String zone30Shape = zone30Path + "zone30.shp";
	static String zone30Links = zone30Path + "zone30Links.shp";
	static boolean considerZone30;
	static Collection<SimpleFeature> featuresInZone30;
	

	public static void main(String[] args) {
//		configFile = "../../runs-svn/detEval/test/input/config.xml";
//		considerZone30 = "true";
		configFile = args[0];
		considerZone30 = Boolean.parseBoolean(args[1]);
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		Controler controler = new Controler(scenario);

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(true);

        if(considerZone30){
			ShapeFileReader shapeFileReader = new ShapeFileReader();
			shapeFileReader.readFileAndInitialize(zone30Shape);
			featuresInZone30 = shapeFileReader.getFeatureSet();
			controler.addControlerListener(new SetLinkAttributesControlerListener (featuresInZone30, zone30Links));
		}
		controler.run();
	}
}