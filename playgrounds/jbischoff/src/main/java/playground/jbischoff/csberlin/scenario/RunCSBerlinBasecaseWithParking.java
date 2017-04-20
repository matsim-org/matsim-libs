/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package playground.jbischoff.csberlin.scenario;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunCSBerlinBasecaseWithParking {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/bmw_carsharing/data/scenario/configBCParking_increase_poster.xml", new DvrpConfigGroup());
		String runId = "bc09_park_poster";
		config.controler().setOutputDirectory("D:/runs-svn/bmw_carsharing/basecase/"+runId);
		config.controler().setRunId(runId);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		Controler controler = new Controler(config);
		SetupParking.installParkingModules(controler);
		
		controler.run();
		
		
	}
}
