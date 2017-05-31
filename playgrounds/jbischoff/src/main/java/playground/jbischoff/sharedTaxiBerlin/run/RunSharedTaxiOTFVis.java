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
package playground.jbischoff.sharedTaxiBerlin.run;

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
public class RunSharedTaxiOTFVis {

	public static void main(String[] args) {

		int capacity = 2;
		int vehicles = 50;

			String runId = "v"+vehicles+"c"+capacity;
			String configFile = "../../../shared-svn/projects/bvg_sharedTaxi/input/config0.1.xml";
			Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
					new OTFVisConfigGroup(), new TaxiFareConfigGroup());
			DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
			drt.setEstimatedBeelineDistanceFactor(1.5);
			drt.setVehiclesFile("vehicles_net_bvg/cap_"+capacity+"/taxis_"+vehicles+".xml.gz");
			drt.setNumberOfThreads(7);
			drt.setMaxTravelTimeAlpha(1.5);
			drt.setMaxTravelTimeBeta(300);
			
			config.controler().setRunId(runId);
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			DrtControlerCreator.createControler(config, true).run();
		
		
		
	}
}
