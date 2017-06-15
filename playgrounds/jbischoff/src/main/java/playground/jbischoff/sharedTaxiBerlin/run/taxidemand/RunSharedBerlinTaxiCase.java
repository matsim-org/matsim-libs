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

/**
 * 
 */
package playground.jbischoff.sharedTaxiBerlin.run.taxidemand;

import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.drt.run.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunSharedBerlinTaxiCase {

	public static void main(String[] args) {

		
			String runId = "s";
			String configFile = "../../../shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/drt/config0.1.xml";
			Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
					new OTFVisConfigGroup(), new TaxiFareConfigGroup());
			DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
			drt.setEstimatedBeelineDistanceFactor(1.5);
			drt.setVehiclesFile("new_net.taxis4to4_cap1.xml");
			drt.setNumberOfThreads(7);
			drt.setMaxTravelTimeAlpha(1.5);
			drt.setMaxTravelTimeBeta(300);
			drt.setkNearestVehicles(7);
			
			config.controler().setRunId(runId);
			config.controler().setOutputDirectory("D:/runs-svn/sharedTaxi/originalScenario");
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			DrtControlerCreator.createControler(config, false).run();
		
	}
		
		
	
}
