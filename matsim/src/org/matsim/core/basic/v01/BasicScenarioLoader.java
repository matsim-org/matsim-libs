/* *********************************************************************** *
 * project: org.matsim.*
 * BasicScenarioLoader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.basic.v01;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.BasicScenario;
import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.xml.sax.SAXException;

/**
 * Implementing a scenario loader for the basic level. This class
 * allows to read each part of the scenario to be read separately.
 * @author dgrether
 * @deprecated use ScenarioLoader instead
 * 
 */
@Deprecated
public class BasicScenarioLoader {

	private static final Logger log = Logger.getLogger(BasicScenarioLoader.class);

	protected Config config;

	private BasicScenario scenario;

	public BasicScenarioLoader(Config config) {
		this.config = config;
		this.scenario = new BasicScenarioImpl(this.config);
	}

	public BasicScenarioLoader(BasicScenario scenario) {
		this.scenario = scenario;
		this.config = this.scenario.getConfig();
	}

	public BasicScenarioLoader(String configFilename) {
		this.config = new Config();
		this.config.addCoreModules();
		MatsimConfigReader reader = new MatsimConfigReader(this.config);
		reader.readFile(configFilename);
		this.scenario = new BasicScenarioImpl(this.config);
	}

	public BasicScenario getScenario() {
		return this.scenario;
	}
	
	protected void setScenario(BasicScenario sc){
		this.scenario = sc;
	}

	/**
	 * Loads the network into the scenario of this class
	 */
	public void loadNetwork() {
		String networkFileName = null;
		if ((this.config.network() != null) && (this.config.network().getInputFile() != null)) {
			networkFileName = this.config.network().getInputFile();
			log.info("loading network from " + networkFileName);
			NetworkLayer network = (NetworkLayer) this.scenario.getNetwork();
			if (this.config.network().isTimeVariantNetwork()) {
				log.info("use TimeVariantLinks in NetworkFactory.");
				network.getFactory().setLinkFactory(new TimeVariantLinkFactory());
			}
				if (network instanceof NetworkLayer) {
					try {
						new MatsimNetworkReader(network).parse(networkFileName);
					} catch (SAXException e) {
						throw new RuntimeException(e);
					} catch (ParserConfigurationException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				} else {
					throw new IllegalStateException(
							"Implementation of Network interface not supported, a specific parser is needed for this implementation of Network interface!");
				}
			if ((config.network().getChangeEventsInputFile() != null) && config.network().isTimeVariantNetwork()) {
				log.info("loading network change events from " + config.network().getChangeEventsInputFile());
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser((NetworkLayer) network);
				try {
					parser.parse(config.network().getChangeEventsInputFile());
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				((NetworkLayer) network).setNetworkChangeEvents(parser.getEvents());
			}
		}
	}

}
