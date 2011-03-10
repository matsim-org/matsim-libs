/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.scenarios;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

public class CreateNetworks {
	private ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	private ConfigReader configReader = new ConfigReader();
			
	public void create(int populationSize, boolean sup) {
		configReader.read();
		String networkfilePath = configReader.getPath() + "/input/PLOC/3towns/network.xml";
		new MatsimNetworkReader(scenario).readFile(networkfilePath);
		
		this.scaleNetwork(populationSize);
		this.write(populationSize, sup);	
	}
	
	private void scaleNetwork(double scaleFactor) {
		for (Link link: this.scenario.getNetwork().getLinks().values()) {
			double oldCapacity = link.getCapacity();
			link.setCapacity(oldCapacity * scaleFactor);
		}		
	}
		
	private void write(int populationSize, boolean sup) {
		new NetworkWriter(scenario.getNetwork()).write(configReader.getPath() + "/input/PLOC/3towns/networks/" + populationSize + "_network.xml");
	}
}
