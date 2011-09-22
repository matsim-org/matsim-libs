/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.svi.converters;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * @author mrieser / senozon
 */
public class Main {

	public static void main(String[] args) {
		convertNetwork(
				"/Volumes/Data/projects/sviDosierungsanlagen/scenarios/kreuzlingen/data1/matsim/network.cleaned.xml", 
				"/Volumes/Data/virtualbox/exchange/dynusTdata");
	}

	private static void convertNetwork(final String networkFilename, final String targetDirectory) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		
		DynusTNetworkWriter writer = new DynusTNetworkWriter(scenario.getNetwork());
		writer.writeToDirectory(targetDirectory);
	}
	
}
