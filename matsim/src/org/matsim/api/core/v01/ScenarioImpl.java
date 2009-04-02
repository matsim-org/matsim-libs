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

package org.matsim.api.core.v01;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.network.BasicLaneDefinitions;
import org.matsim.core.basic.network.BasicLaneDefinitionsImpl;
import org.matsim.core.basic.signalsystems.BasicSignalSystems;
import org.matsim.core.basic.signalsystems.BasicSignalSystemsImpl;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.core.basic.signalsystemsconfig.BasicSignalSystemConfigurationsImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.FacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimLaneDefinitionsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkChangeEventsParser;
import org.matsim.core.network.NetworkFactory;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.signalsystems.MatsimSignalSystemConfigurationsReader;
import org.matsim.signalsystems.MatsimSignalSystemsReader;
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
public class ScenarioImpl implements Scenario {
	private final String worldFileName;
	private final String networkFileName;
	private final String facilitiesFileName;
	private final String populationFileName;
	private boolean isTimeVariantNetwork = false;
	private String networkChangeEventsInputFile;

	private boolean worldLoaded = false;
	private boolean networkLoaded = false;
	private boolean facilitiesLoaded = false;
	private boolean populationLoaded = false;

	private World world = null;
	private NetworkLayer network = null;
	private Facilities facilities = null;
	private Population population = null;
	private BasicLaneDefinitions laneDefinitions = null;
	private BasicSignalSystems signalSystems = null;
	private BasicSignalSystemConfigurations signalSystemConfigurations = null;

	private final NetworkFactory networkFactory;
	private Config config;

	private static final Logger log = Logger.getLogger(ScenarioImpl.class);

	public ScenarioImpl() {
		this.world = new World();
		this.worldFileName = null;
		this.worldLoaded = true;
		this.network = new NetworkLayer();
		this.networkFactory = this.network.getFactory();
		this.networkFileName = null;
		this.networkLoaded = true;
		this.facilities = new FacilitiesImpl();
		this.facilitiesFileName = null;
		this.facilitiesLoaded = true;
		this.population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		this.populationFileName = null;
		this.populationLoaded = true;
		this.config = new Config();
	}
	
	/**
	 * Loads the data from the locations specified in the configuration.
	 * The configuration must already be loaded at this point.
	 *
	 * @param config
	 */
	public ScenarioImpl(final Config config) {
		this(config, (NetworkFactory)null);
	}

	public ScenarioImpl(final Config config, final NetworkFactory factory) {
		this.worldFileName = config.world().getInputFile();
		this.networkFileName = config.network().getInputFile();
		this.isTimeVariantNetwork = config.network().isTimeVariantNetwork();
		this.networkChangeEventsInputFile = config.network().getChangeEventsInputFile();
		this.facilitiesFileName = config.facilities().getInputFile();
		this.populationFileName = config.plans().getInputFile();
		this.config = config;
		if (factory == null) {
			this.networkFactory = new NetworkFactory();
			if (this.isTimeVariantNetwork){
				this.networkFactory.setLinkFactory(new TimeVariantLinkFactory());
			}
		} else {
			this.networkFactory = factory;
		}
	}

	public ScenarioImpl(final String worldFileName, final String networkFileName,
			final String facilitiesFileName, final String populationFileName) {
		this.worldFileName = worldFileName;
		this.networkFileName = networkFileName;
		this.facilitiesFileName = facilitiesFileName;
		this.populationFileName = populationFileName;
		this.networkFactory = new NetworkFactory();
	}

	public ScenarioImpl(Config config, NetworkLayer network) {
		this(config, network.getFactory());
		this.network = network;
	}

	public World getWorld() throws RuntimeException {
		if (!this.worldLoaded) {
			if (this.worldFileName != null) {
				log.info("loading world from " + this.worldFileName);
				this.world = Gbl.getWorld();
				try {
					new MatsimWorldReader(this.world).parse(this.worldFileName);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				this.world = Gbl.getWorld();
			}
			this.worldLoaded = true;
		}
		return this.world;
	}

	public NetworkLayer getNetwork() throws RuntimeException {
		if (!this.networkLoaded) {
			getWorld(); // make sure the world is loaded
			log.info("loading network from " + this.networkFileName);
			this.network = new NetworkLayer(this.networkFactory);

			try {
				new MatsimNetworkReader(this.network).parse(this.networkFileName);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			this.world.setNetworkLayer(this.network);
			this.world.complete();

			if ((this.networkChangeEventsInputFile != null) && this.isTimeVariantNetwork){
				log.info("loading network change events from " + this.networkChangeEventsInputFile);
				NetworkChangeEventsParser parser = new NetworkChangeEventsParser(this.network);
				try {
					parser.parse(this.networkChangeEventsInputFile);
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

	public Facilities getFacilities() throws RuntimeException {
		if (!this.facilitiesLoaded) {
			if (this.facilitiesFileName != null) {
				getWorld(); // make sure the world is loaded
				log.info("loading facilities from " + this.facilitiesFileName);
				this.facilities = (Facilities)this.world.createLayer(Facilities.LAYER_TYPE, null);
				try {
					new MatsimFacilitiesReader(this.facilities).parse(this.facilitiesFileName);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				this.facilities = (Facilities)this.world.createLayer(Facilities.LAYER_TYPE, null);
			}
			this.world.complete();
			this.facilitiesLoaded = true;
		}
		return this.facilities;
	}

	public Population getPopulation() throws RuntimeException {
		if (!this.populationLoaded) {
			this.population = new PopulationImpl(PopulationImpl.NO_STREAMING);
			// make sure that world, facilities and network are loaded as well
			getWorld();
			getNetwork();
			getFacilities();

			log.info("loading population from " + this.populationFileName);
			try {
				new MatsimPopulationReader(this.population, this.network).parse(this.populationFileName);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			this.population.printPlansCount();

			this.populationLoaded = true;
		}
		return this.population;
	}
	
	public BasicLaneDefinitions getLaneDefinitions(){
		if ((this.laneDefinitions == null) && (this.config.network().getLaneDefinitionsFile() != null)){
			this.laneDefinitions = new BasicLaneDefinitionsImpl();
			MatsimLaneDefinitionsReader reader = new MatsimLaneDefinitionsReader(this.laneDefinitions);
			reader.readFile(this.config.network().getLaneDefinitionsFile());
		}
		return this.laneDefinitions;
	}

	public BasicSignalSystems getSignalSystems() {
		if ((this.config == null) || (this.config.signalSystems() == null)){
			throw new IllegalStateException("SignalSystems can only be loaded if set in config");
		}
		if (this.config.signalSystems().getSignalSystemFile() != null){
			//we try to parse the deprecated format
			if ((this.signalSystems == null) && (this.laneDefinitions == null)) {
				this.laneDefinitions = new BasicLaneDefinitionsImpl();
				this.signalSystems = new BasicSignalSystemsImpl();
				MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(this.laneDefinitions, this.signalSystems);
				log.info("loading signalsystems from " + this.config.signalSystems().getSignalSystemFile());
				reader.readFile(this.config.signalSystems().getSignalSystemFile());
			}
			else if (this.signalSystems == null){
				this.signalSystems = new BasicSignalSystemsImpl();
				MatsimSignalSystemsReader reader = new MatsimSignalSystemsReader(this.signalSystems);
				log.info("loading signalsystems from " + this.config.signalSystems().getSignalSystemFile());
				reader.readFile(this.config.signalSystems().getSignalSystemFile());
			}
		}
		return this.signalSystems;
	}

	public BasicSignalSystemConfigurations getSignalSystemsConfiguration() {
		if ((this.config == null) || (this.config.signalSystems() == null)){
			throw new IllegalStateException("SignalSystems can only be loaded if set in config");
		}
		if (this.config.signalSystems().getSignalSystemConfigFile() != null){
			if (this.signalSystemConfigurations == null){
				this.signalSystemConfigurations = new BasicSignalSystemConfigurationsImpl();
				MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(this.signalSystemConfigurations);
				log.info("loading signalsystemsconfiguration from " + this.config.signalSystems().getSignalSystemConfigFile());
				reader.readFile(this.config.signalSystems().getSignalSystemConfigFile());
			}
		}
		return this.signalSystemConfigurations;
	}

	public Config getConfig() {
		return this.config;
	}

	public Coord createCoord(final double d, final double e) {
		return new CoordImpl( d, e ) ;
	}

	public Id createId(final String string) {
		return new IdImpl( string) ;
	}

}
