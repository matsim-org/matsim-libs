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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;

public class CreateNetworks {
	private ScenarioImpl scenario = new ScenarioImpl();
	private final static Logger log = Logger.getLogger(CreateNetworks.class);
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
