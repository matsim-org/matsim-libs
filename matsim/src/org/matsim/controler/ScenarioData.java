/* *********************************************************************** *
 * project: org.matsim.*
 * Scenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.controler;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkChangeEventsParser;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.xml.sax.SAXException;

/**
 * Provides convenient methods to load often used data. The methods ensure that possibly
 * dependent data is loaded as well (e.g. network requires the world to be loaded first,
 * plans can depend upon world, network, facilities).
 *
 * @author mrieser
 */
public class ScenarioData {
	private final String worldFileName;
	private final String networkFileName;
	private final String facilitiesFileName;
	private final String populationFileName;
	private boolean isTimeVariantNetwork;
	private String changEventsInputFile;
	
	private boolean worldLoaded = false;
	private boolean networkLoaded = false;
	private boolean facilitiesLoaded = false;
	private boolean populationLoaded = false;

	private World world = null;
	private NetworkLayer network = null;
	private Facilities facilities = null;
	private Population population = null;
	
	

	private static final Logger log = Logger.getLogger(ScenarioData.class);

	/**
	 * Loads the data from the locations specified in the configuration.
	 * The configuration must already be loaded at this point.
	 *
	 * @param config
	 */
	public ScenarioData(final Config config) {
		this.worldFileName = config.world().getInputFile();
		this.networkFileName = config.network().getInputFile();
		this.isTimeVariantNetwork = config.network().isTimeVariantNetwork();
		this.changEventsInputFile = config.network().getChangeEventsInputFile();
		this.facilitiesFileName = config.facilities().getInputFile();
		this.populationFileName = config.plans().getInputFile();
	}

	public ScenarioData(final String worldFileName, final String networkFileName,
			final String facilitiesFileName, final String populationFileName) {
		this.worldFileName = worldFileName;
		this.networkFileName = networkFileName;
		this.facilitiesFileName = facilitiesFileName;
		this.populationFileName = populationFileName;
	}

	public World getWorld() {
		if (!this.worldLoaded) {
			if (this.worldFileName != null) {
				log.info("loading world from " + this.worldFileName);
				this.world = Gbl.getWorld();
				final MatsimWorldReader worldReader = new MatsimWorldReader(this.world);
				worldReader.readFile(this.worldFileName);
			} else {
				this.world = Gbl.getWorld();
			}
			this.worldLoaded = true;
		}
		return this.world;
	}

	public NetworkLayer getNetwork() {
		if (!this.networkLoaded) {
			getWorld(); // make sure the world is loaded
			log.info("loading network from " + this.networkFileName);
			NetworkFactory nf = new NetworkFactory();			
			if (this.isTimeVariantNetwork){
				nf.setLinkPrototype(TimeVariantLinkImpl.class);
			}
			this.network = new NetworkLayer(nf);	
			
			new MatsimNetworkReader(this.network).readFile(this.networkFileName);
			this.world.setNetworkLayer(this.network);
			this.world.complete();
			
			if (this.changEventsInputFile != null && this.isTimeVariantNetwork){
				log.info("loading change events from " + this.changEventsInputFile);
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser(this.network);
				try {
					parser.parse(this.changEventsInputFile);
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.network.setNetworkChangeEvents(parser.getEvents()); 
			}
			
			this.networkLoaded = true;
		}
		return this.network;
	}
	
	public Facilities getFacilities() {
		if (!this.facilitiesLoaded) {
			if (this.facilitiesFileName != null) {
				getWorld(); // make sure the world is loaded
				log.info("loading facilities from " + this.facilitiesFileName);
				this.facilities = (Facilities)this.world.createLayer(Facilities.LAYER_TYPE, null);
				new MatsimFacilitiesReader(this.facilities).readFile(this.facilitiesFileName);
			} else {
				this.facilities = (Facilities)this.world.createLayer(Facilities.LAYER_TYPE, null);
			}
			this.world.complete();
			this.facilitiesLoaded = true;
		}
		return this.facilities;
	}

	public Population getPopulation() {
		if (!this.populationLoaded) {
			this.population = new Population(Population.NO_STREAMING);
			// make sure that world, facilities and network are loaded as well
			getWorld();
			getNetwork();
			getFacilities();

			log.info("loading population from " + this.populationFileName);
			PopulationReader plansReader = new MatsimPlansReader(this.population);
			plansReader.readFile(this.populationFileName);
			this.population.printPlansCount();

			this.populationLoaded = true;
		}
		return this.population;
	}
}
