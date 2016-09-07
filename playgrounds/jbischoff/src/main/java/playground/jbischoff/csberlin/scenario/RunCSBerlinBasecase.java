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

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunCSBerlinBasecase {
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("../../../shared-svn/projects/bmw_carsharing/data/scenario/configBC.xml");
		String runId = "bc09_nopark";
		config.controler().setOutputDirectory("D:/runs-svn/bmw_carsharing/basecase/"+runId);
		config.controler().setRunId(runId);
		
		
		Controler controler = new Controler(config);
		controler.run();
		
		
	}
}
