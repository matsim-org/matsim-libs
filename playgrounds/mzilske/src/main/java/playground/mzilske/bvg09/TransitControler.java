/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
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

package playground.mzilske.bvg09;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.queuesim.TransitQueueSimulation;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.transitSchedule.TransitScheduleReaderV1;
import org.matsim.vehicles.BasicVehicleReaderV1;
import org.xml.sax.SAXException;

import playground.dgrether.utils.LogOutputEventHandler;
import playground.mrieser.pt.config.TransitConfigGroup;
import playground.mrieser.pt.replanning.TransitStrategyManagerConfigLoader;
import playground.mrieser.pt.router.PlansCalcTransitRoute;

/**
 * @author mrieser
 */
public class TransitControler extends Controler {

	private final TransitConfigGroup transitConfig;

	public TransitControler(final String[] args) {
		super(args);
		this.transitConfig = new TransitConfigGroup();
		init();
	}

	public TransitControler(final ScenarioImpl scenario) {
		super(scenario);
		this.transitConfig = new TransitConfigGroup();
		init();
	}

	private final void init() {
		if (this.config.getModule(TransitConfigGroup.GROUP_NAME) == null) {
			this.config.addModule(TransitConfigGroup.GROUP_NAME, this.transitConfig);
		} else {
			// this would not be necessary if TransitConfigGroup is part of core config
			Module oldModule = this.config.getModule(TransitConfigGroup.GROUP_NAME);
			this.config.removeModule(TransitConfigGroup.GROUP_NAME);
			this.transitConfig.addParam("transitScheduleFile", oldModule.getValue("transitScheduleFile"));
			this.transitConfig.addParam("vehiclesFile", oldModule.getValue("vehiclesFile"));
			this.transitConfig.addParam("transitModes", oldModule.getValue("transitModes"));
		}
		this.config.scenario().setUseTransit(true);
		this.config.scenario().setUseVehicles(true);
		Set<EventsFileFormat> formats = EnumSet.copyOf(this.config.controler().getEventsFileFormats());
		formats.add(EventsFileFormat.xml);
		this.config.controler().setEventsFileFormats(formats);

		ActivityParams params = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		params.setTypicalDuration(120.0);
		this.config.charyparNagelScoring().addActivityParams(params);

		this.getNetwork().getFactory().setRouteFactory(TransportMode.pt, new ExperimentalTransitRouteFactory());

		TransitControlerListener cl = new TransitControlerListener(this.transitConfig);
		addControlerListener(cl);
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		TransitStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}

	@Override
	protected void runMobSim() {
		TransitQueueSimulation sim = new TransitQueueSimulation(this.scenarioData, this.events);
		sim.setUseUmlaeufe(true);
		sim.startOTFServer("livesim");
		OTFDemo.ptConnect("livesim", this.config);
		sim.run();
	}

	@Override
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {
		return new PlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes,
				this.getLeastCostPathCalculatorFactory(), this.scenarioData.getTransitSchedule(), this.transitConfig);
	}

	public static class TransitControlerListener implements StartupListener {

		private final TransitConfigGroup config;

		public TransitControlerListener(final TransitConfigGroup config) {
			this.config = config;
		}

		public void notifyStartup(final StartupEvent event) {
			if (this.config.getTransitScheduleFile() != null) {
				try {
					new TransitScheduleReaderV1(event.getControler().getScenario().getTransitSchedule(), event.getControler().getScenario().getNetwork()).readFile(this.config.getTransitScheduleFile());
				} catch (SAXException e) {
					throw new RuntimeException("could not read transit schedule.", e);
				} catch (ParserConfigurationException e) {
					throw new RuntimeException("could not read transit schedule.", e);
				} catch (IOException e) {
					throw new RuntimeException("could not read transit schedule.", e);
				}
			}
			if (this.config.getVehiclesFile() != null) {
				try {
					new BasicVehicleReaderV1(event.getControler().getScenario().getVehicles()).parse(this.config.getVehiclesFile());
				} catch (SAXException e) {
					throw new RuntimeException("could not read vehicles.", e);
				} catch (ParserConfigurationException e) {
					throw new RuntimeException("could not read vehicles.", e);
				} catch (IOException e) {
					throw new RuntimeException("could not read vehicles.", e);
				}
			}
			ReconstructingUmlaufBuilder reconstructingUmlaufBuilder = new ReconstructingUmlaufBuilder(
					event.getControler().getScenario().getNetwork(), event
							.getControler().getScenario()
							.getTransitSchedule().getTransitLines().values(),
					event.getControler().getScenario().getVehicles());
			reconstructingUmlaufBuilder.build();
		}

	}

	public static void main(final String[] args) {
		TransitControler tc = new TransitControler(args);
		tc.setOverwriteFiles(true);
		tc.run();
	}

}
