/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationDelayController.java
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
package playground.gregor.sims.run.deprecated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.evacuation.base.Building;
import org.matsim.evacuation.base.BuildingsShapeReader;
import org.matsim.evacuation.base.EvacuationNetFromNetcdfGenerator;
import org.matsim.evacuation.base.EvacuationNetGenerator;
import org.matsim.evacuation.base.EvacuationPlansGenerator;
import org.matsim.evacuation.base.NetworkChangeEventsFromNetcdf;
import org.matsim.evacuation.config.EvacuationConfigGroup;
import org.matsim.evacuation.config.EvacuationConfigGroup.EvacuationScenario;
import org.matsim.evacuation.flooding.FloodingReader;
import org.matsim.evacuation.riskaversion.RiskCostFromFloodingData;
import org.matsim.evacuation.run.EvacuationQSimControllerII;
import org.matsim.evacuation.shelters.EvacuationShelterNetLoader;
import org.matsim.evacuation.shelters.signalsystems.ShelterDoorBlockerSetup;
import org.matsim.evacuation.shelters.signalsystems.ShelterInputCounterSignalSystems;
import org.matsim.evacuation.socialcost.SocialCostCalculatorSingleLink;
import org.matsim.evacuation.travelcosts.PluggableTravelCostCalculator;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;

import playground.gregor.sims.evacuationdelay.DelayedEvacuationPopulationLoader;

@Deprecated
public class EvacuationDelayController extends Controler {

	final private static Logger log = Logger.getLogger(EvacuationQSimControllerII.class);

	private List<Building> buildings;

	private List<FloodingReader> netcdfReaders = null;

	private EvacuationShelterNetLoader esnl = null;

	private HashMap<Id, Building> shelterLinkMapping = null;

	PluggableTravelCostCalculator pluggableTravelCost = null;

	private EvacuationConfigGroup ec;

	public EvacuationDelayController(String[] args) {
		super(args);
		setOverwriteFiles(true);
		this.config.scenario().setUseSignalSystems(true);
		this.config.scenario().setUseLanes(true);
		this.config.addQSimConfigGroup(new QSimConfigGroup());
	}

	@Override
	protected void setUp() {
		super.setUp();

		if (this.ec.isLoadShelters()) {
			loadShelterSignalSystems();
		}

		if (this.ec.isSocialCostOptimization()) {
			initSocialCostOptimization();
		}

		if (this.ec.isRiskMinimization()) {
			initRiskMinimization();
		}

		unloadNetcdfReaders();

	}

	private void initSocialCostOptimization() {
		initPluggableTravelCostCalculator();
		SocialCostCalculatorSingleLink sc = new SocialCostCalculatorSingleLink(this.network, this.config.travelTimeCalculator().getTraveltimeBinSize(), getEvents());
		this.pluggableTravelCost.addTravelCost(sc);
		this.events.addHandler(sc);
		this.strategyManager = loadStrategyManager();
		addControlerListener(sc);
	}

	private void initRiskMinimization() {
		initPluggableTravelCostCalculator();
		loadNetcdfReaders();

		RiskCostFromFloodingData rc = new RiskCostFromFloodingData(this.network, this.netcdfReaders, getEvents(), this.ec.getBufferSize());
		this.pluggableTravelCost.addTravelCost(rc);
		this.events.addHandler(rc);
	}

	private void initPluggableTravelCostCalculator() {
		if (this.pluggableTravelCost == null) {
			if (this.travelTimeCalculator == null) {
				this.travelTimeCalculator = getTravelTimeCalculatorFactory().createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
			}
			this.pluggableTravelCost = new PluggableTravelCostCalculator(this.travelTimeCalculator);
			setTravelCostCalculatorFactory(new TravelCostCalculatorFactory() {

				// This is thread-safe because pluggableTravelCost is
				// thread-safe.

				@Override
				public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime timeCalculator, CharyparNagelScoringConfigGroup cnScoringGroup) {
					return EvacuationDelayController.this.pluggableTravelCost;
				}

			});
		}
	}

	private void loadShelterSignalSystems() {
		this.config.network().setLaneDefinitionsFile("nullnull");

		ShelterInputCounterSignalSystems sic = new ShelterInputCounterSignalSystems(this.scenarioData, this.shelterLinkMapping);
		this.events.addHandler(sic);
		getQueueSimulationListener().add(sic);

		ShelterDoorBlockerSetup shelterSetup = new ShelterDoorBlockerSetup(sic);
		final SignalSystemsManager signalManager = shelterSetup.createSignalManager(getScenario());
		signalManager.setEventsManager(this.events);
		getQueueSimulationListener().add(new QSimSignalEngine(signalManager));
		addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				signalManager.resetModel(event.getIteration());
			}
		});
	}

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

	@Override
	protected void loadData() {
		super.loadData();

		Module m = this.config.getModule("evacuation");
		this.ec = new EvacuationConfigGroup(m);
		this.config.getModules().put("evacuation", this.ec);
		// network
		NetworkImpl net = this.scenarioData.getNetwork();

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
			loadNetWorkChangeEvents(net);
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
				new DelayedEvacuationPopulationLoader(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, this.netcdfReaders).getPopulation();
			} else {
				new DelayedEvacuationPopulationLoader(this.scenarioData.getPopulation(), this.buildings, this.scenarioData, null).getPopulation();
			}
		} else {
			if (this.ec.getEvacuationScanrio() != EvacuationScenario.night) {
				throw new RuntimeException("Evacuation simulation from plans file so far only works for the night scenario.");
			}
			new EvacuationPlansGenerator(this.population, this.network, this.network.getLinks().get(new IdImpl("el1"))).run();
		}

		this.population = this.scenarioData.getPopulation();

		if (this.ec.isLoadShelters()) {
			this.esnl.generateShelterLinks();
		}
	}

	public static void main(final String[] args) {
		final Controler controler = new EvacuationDelayController(args);
		controler.run();
		System.exit(0);
	}
}
