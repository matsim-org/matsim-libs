/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Class that consolidates matsim.config and scenario.
 * 
 * @author sschroeder
 *
 */
public class MatsimStuffLoader {
	
	/**
	 * Class to store Matsim.Config and Matsim.Scenario
	 * 
	 * @author stefan
	 *
	 */
	public static class MatsimStuff {
		public final Config config;
		public final Scenario scenario;
		
		public MatsimStuff(Config config, Scenario scenario) {
			super();
			this.config = config;
			this.scenario = scenario;
		}
		
		public Network getNetwork(){
			return scenario.getNetwork();
		}
	}
	
	/**
	 * Reads and creates a network and sets up the required matsim.config and matsim.scenario.
	 * 
	 * <p>The network can be retrieved by calling <code>matsimStuff.scenario.getNetwork();</code>
	 * 
	 * @param networkFilename
	 * @return
	 */
	public static MatsimStuff loadNetworkAndGetStuff(String networkFilename){
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
		return new MatsimStuff(config, scenario);
	}

}
