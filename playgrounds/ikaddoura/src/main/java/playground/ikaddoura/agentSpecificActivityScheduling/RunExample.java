/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;

/**
* @author ikaddoura
*/

public class RunExample {

	private static String configFile = "../../../runs-svn/berlin-dz-time/input/config.xml";

	public static void main(String[] args) throws IOException {
		
		Config config = ConfigUtils.loadConfig(configFile, new AgentSpecificActivitySchedulingConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		AgentSpecificActivitySchedulingConfigGroup asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) scenario.getConfig().getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);
		asasConfigGroup.setTolerance(0.);
		
		controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		MATSimVideoUtils.createLegHistogramVideo(controler.getConfig().controler().getOutputDirectory());
	}

}

