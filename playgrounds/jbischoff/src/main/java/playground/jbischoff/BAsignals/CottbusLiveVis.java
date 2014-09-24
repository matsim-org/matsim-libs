/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusLiveVis
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
package playground.jbischoff.BAsignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.mobsim.QSimSignalEngine;
import org.matsim.signalsystems.mobsim.SignalEngine;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;

import playground.jbischoff.BAsignals.builder.JbSignalBuilder;
import playground.jbischoff.BAsignals.model.AdaptiveControllHead;
import playground.jbischoff.BAsignals.model.CarsOnLaneHandler;


/**
 * @author dgrether
 *
 */
public class CottbusLiveVis {

	private final String config = "/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/config_slv_l.xml";

	
	private void runCottbus() {
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(config));
		
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new XY2Links(scenario));
//		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
		final FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								scenario ).instantiateAndConfigureTripRouter(
										new RoutingContextImpl(
												timeCostCalc,
												timeCostCalc ) ) ) );
		((PopulationImpl)scenario.getPopulation()).runAlgorithms();
		
		EventsManager events = EventsUtils.createEventsManager();
//		OutputDirectoryHierarchy controlerIO = new OutputDirectoryHierarchy(scenario.getConfig().controler().getOutputDirectory(), false);
		QSim qSim = (QSim) new QSimFactory().createMobsim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			AdaptiveControllHead adaptiveControllHead = new AdaptiveControllHead();
			CarsOnLaneHandler carsOnLaneHandler = new CarsOnLaneHandler();
			carsOnLaneHandler.setAdaptiveControllHead(adaptiveControllHead);
			JbSignalBuilder jbBuilder = new JbSignalBuilder((SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME), new FromDataBuilder(scenario, events), carsOnLaneHandler, adaptiveControllHead);
			SignalEngine engine = new QSimSignalEngine(jbBuilder.createAndInitializeSignalSystemsManager());
			qSim.addQueueSimulationListeners(engine);
		}
		OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(), scenario, events, qSim);
		OTFClientLive.run(scenario.getConfig(), server);

		qSim.run();
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CottbusLiveVis().runCottbus();
	}


}
