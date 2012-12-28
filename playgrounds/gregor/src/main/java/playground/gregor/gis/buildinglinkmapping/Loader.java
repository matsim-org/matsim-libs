/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.gis.buildinglinkmapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.evacuation.base.Building;
import org.matsim.contrib.evacuation.base.BuildingsShapeReader;
import org.matsim.contrib.evacuation.base.EvacuationNetFromNetcdfGenerator;
import org.matsim.contrib.evacuation.base.EvacuationNetGenerator;
import org.matsim.contrib.evacuation.base.EvacuationPlansGenerator;
import org.matsim.contrib.evacuation.base.EvacuationPopulationFromShapeFileLoader;
import org.matsim.contrib.evacuation.base.NetworkChangeEventsFromNetcdf;
import org.matsim.contrib.evacuation.config.EvacuationConfigGroup;
import org.matsim.contrib.evacuation.config.EvacuationConfigGroup.EvacuationScenario;
import org.matsim.contrib.evacuation.flooding.FloodingReader;
import org.matsim.contrib.evacuation.shelters.EvacuationShelterNetLoader;
import org.matsim.contrib.evacuation.travelcosts.PluggableTravelCostCalculator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;

public class Loader {
	
	
	final private static Logger log = Logger.getLogger(Loader.class);
	
	private EvacuationConfigGroup ec;
	private final Scenario scenarioData;
	private Population population;

	private List<Building> buildings;

	private List<FloodingReader> netcdfReaders = null;

	private EvacuationShelterNetLoader esnl = null;

	private HashMap<Id, Building> shelterLinkMapping = null;

	PluggableTravelCostCalculator pluggableTravelCost = null;

	private final Config config;
	
	public Loader(Scenario scenarioData) {
		this.config = scenarioData.getConfig();
		this.scenarioData = scenarioData;
	}
	
//	public void setUp() {
//
//
//		if (this.ec.isLoadShelters()) {
//			loadShelterSignalSystems();
//		}
//
//		if (this.ec.isSocialCostOptimization()) {
//			initSocialCostOptimization();
//		}
//
//		if (this.ec.isRiskMinimization()) {
//			initRiskMinimization();
//		}
//
//		unloadNetcdfReaders();
//
//	}

//	private void initSocialCostOptimization() {
//		initPluggableTravelCostCalculator();
//		SocialCostCalculatorSingleLink sc = new SocialCostCalculatorSingleLink(this.network, getConfig().travelTimeCalculator().getTraveltimeBinSize(), getEvents());
//		this.pluggableTravelCost.addTravelCost(sc);
//		this.events.addHandler(sc);
//		this.strategyManager = loadStrategyManager();
//		addControlerListener(sc);
//	}

//	private void initRiskMinimization() {
//		initPluggableTravelCostCalculator();
//		loadNetcdfReaders();
//
//		RiskCostFromFloodingData rc = new RiskCostFromFloodingData(this.network, this.netcdfReaders, getEvents(), this.ec.getBufferSize());
//		this.pluggableTravelCost.addTravelCost(rc);
//		this.events.addHandler(rc);
//	}

//	private void initPluggableTravelCostCalculator() {
//		if (this.pluggableTravelCost == null) {
//			if (this.travelTimeCalculator == null) {
//				this.travelTimeCalculator = getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
//			}
//			this.pluggableTravelCost = new PluggableTravelCostCalculator(this.travelTimeCalculator);
//			setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {
//
//				// This is thread-safe because pluggableTravelCost is
//				// thread-safe.
//
//				@Override
//				public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
//					return EvacuationQSimControllerII.this.pluggableTravelCost;
//				}
//
//			});
//		}
//	}

//	private void loadShelterSignalSystems() {
//		this.config.network().setLaneDefinitionsFile("nullnull");
//
//		ShelterInputCounterSignalSystems sic = new ShelterInputCounterSignalSystems(this.scenarioData, this.shelterLinkMapping);
//		this.events.addHandler(sic);
//		getQueueSimulationListener().add(sic);
//
//		ShelterDoorBlockerSetup shelterSetup = new ShelterDoorBlockerSetup(sic);
//		final SignalSystemsManager signalManager = shelterSetup.createSignalManager(getScenario());
//		signalManager.setEventsManager(this.events);
//		getQueueSimulationListener().add(new QSimSignalEngine(signalManager));
//
//		addControlerListener(new IterationStartsListener() {
//			@Override
//			public void notifyIterationStarts(IterationStartsEvent event) {
//				signalManager.resetModel(event.getIteration());
//			}
//		});
//	}

	private void unloadNetcdfReaders() {
		this.netcdfReaders = null;
		log.info("netcdf readers destroyed");
	}

	private void loadNetcdfReaders() {
		if (this.netcdfReaders != null) {
			return;
		}
		log.info("loading netcdf readers");
		int count = this.ec.getSWWFileCount();
		if (count <= 0) {
			return;
		}
		this.netcdfReaders = new ArrayList<FloodingReader>();
		double offsetEast = this.ec.getSWWOffsetEast();
		double offsetNorth = this.ec.getSWWOffsetNorth();
		for (int i = 0; i < count; i++) {
			String netcdf = this.ec.getSWWRoot() + "/" + this.ec.getSWWFilePrefix() + i + this.ec.getSWWFileSuffix();
			FloodingReader fr = new FloodingReader(netcdf);
			fr.setReadTriangles(true);
			fr.setOffset(offsetEast, offsetNorth);
			this.netcdfReaders.add(fr);
		}
		log.info("done.");
	}

	private void loadNetWorkChangeEvents(NetworkImpl net) {
		loadNetcdfReaders();
		if (this.netcdfReaders == null) {
			throw new RuntimeException("No netcdf reader could be loaded!");
		} else if (!net.getFactory().isTimeVariant()) {
			throw new RuntimeException("Network layer is not time variant!");
		} else if (net.getNetworkChangeEvents() != null) {
			throw new RuntimeException("Network change events allready loaded!");
		}
		List<NetworkChangeEvent> events = new NetworkChangeEventsFromNetcdf(this.netcdfReaders, this.scenarioData).createChangeEvents();
		net.setNetworkChangeEvents(events);
	}

	public void loadData() {

		Module m = this.config.getModule("evacuation");
		this.ec = new EvacuationConfigGroup(m);
		this.config.getModules().put("evacuation", this.ec);
		
		this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
		if (true)return;
		
		EvacuationScenario sc = this.ec.getEvacuationScanrio();

		if (sc == EvacuationScenario.from_file) {
			return;
		}

		// network
		Network net = this.scenarioData.getNetwork();

		if (this.ec.isLoadShelters()) {
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
			}
			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
			}
			this.esnl = new EvacuationShelterNetLoader(this.buildings, this.scenarioData, this.netcdfReaders);
			net = this.esnl.getNetwork();
			this.shelterLinkMapping = this.esnl.getShelterLinkMapping();

		} else {
			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				loadNetcdfReaders();
				new EvacuationNetFromNetcdfGenerator(net, this.scenarioData.getConfig(), this.netcdfReaders).run();
			} else {
				new EvacuationNetGenerator(net, this.config).run();
			}
		}

		if (this.scenarioData.getConfig().network().isTimeVariantNetwork() && this.ec.isGenerateEvacNetFromSWWFile()) {
			loadNetWorkChangeEvents((NetworkImpl) net);
		}

		if (this.ec.isLoadPopulationFromShapeFile()) {
			if (this.scenarioData.getPopulation().getPersons().size() > 0) {
				throw new RuntimeException("Population already loaded. In order to load population from shape file, the population input file paramter in the population section of the config.xml must not be set!");
			}
			// population
			if (this.buildings == null) {
				this.buildings = BuildingsShapeReader.readDataFile(this.ec.getBuildingsFile(), this.ec.getSampleSize());
			}

			if (this.ec.isGenerateEvacNetFromSWWFile()) {
				new EvacuationPopulationFromShapeFileLoader(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, this.netcdfReaders).getPopulation();
			} else {
				new EvacuationPopulationFromShapeFileLoader(this.scenarioData.getPopulation(), this.buildings, this.scenarioData).getPopulation();
			}
		} else {
			if (sc != EvacuationScenario.night) {
				throw new RuntimeException("Evacuation simulation from plans file so far only works for the night scenario.");
			}
			new EvacuationPlansGenerator(this.population, this.scenarioData.getNetwork(), this.scenarioData.getNetwork().getLinks().get(new IdImpl("el1"))).run();
		}

		this.population = this.scenarioData.getPopulation();

		if (this.ec.isLoadShelters()) {
			this.esnl.generateShelterLinks();
		}
		
		unloadNetcdfReaders();
	}
	
	public List<Building> getBuildings() {
		return this.buildings;
	}

}
