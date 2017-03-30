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
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.michalm.drt.run.DrtConfigGroup;
import playground.michalm.drt.run.RunSharedTaxiBerlin;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunSharedTaxiBatch {

	public static void main(String[] args) {

		int capacity = 3;
		for (int i = 25; i<=200; i=i+25){
			String runId = "v"+i+"c"+capacity;
			String configFile = "../../../shared-svn/projects/bvg_sharedTaxi/input/config.xml";
			Config config = ConfigUtils.loadConfig(configFile, new DvrpConfigGroup(), new DrtConfigGroup(),
					new OTFVisConfigGroup(), new TaxiFareConfigGroup());
			DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
			drt.setVehiclesFile("vehicles/cap_"+capacity+"/taxis_"+i+".xml.gz");
			config.controler().setRunId(runId);
			config.controler().setOutputDirectory("D:/runs-svn/bvg_sharedTaxi/demand01/"+runId);
			RunSharedTaxiBerlin.createControler(config, false).run();
		}
		
	}
}
