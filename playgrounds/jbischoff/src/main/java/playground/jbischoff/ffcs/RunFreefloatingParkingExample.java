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
package playground.jbischoff.ffcs;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.ffcs.sim.SetupFreefloatingParking;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunFreefloatingParkingExample {
public static void main(String[] args) {
	Config config = ConfigUtils.loadConfig("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/example/config_ffcs.xml", new FFCSConfigGroup(), new DvrpConfigGroup());
	config.plans().setInputFile("populationffcs100.xml");
	config.facilities().setInputFile("parkingFacilities.xml");
	config.controler().setOutputDirectory("../../../shared-svn/projects/bmw_carsharing/example/ffcs_output");
	config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
	config.controler().setLastIteration(10);
	config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
	
	Scenario scenario = ScenarioUtils.loadScenario(config);
	Controler controler = new Controler(scenario);
//    controler.addOverridingModule(new OTFVisLive	Module());
	SetupFreefloatingParking.installFreefloatingParkingModules(controler, (FFCSConfigGroup) config.getModule("freefloating"));
	controler.run();

}
}
