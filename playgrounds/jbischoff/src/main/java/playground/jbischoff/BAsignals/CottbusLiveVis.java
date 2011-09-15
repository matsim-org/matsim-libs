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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.run.OTFVis;
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
		ScenarioLoaderImpl loader = ScenarioLoaderImpl.createScenarioLoaderImplAndResetRandomSeed(config);
		loader.loadScenario();
		Scenario scenario = loader.getScenario();
		
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new XY2Links((NetworkImpl) scenario.getNetwork()));
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
		final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(scenario.getConfig().planCalcScore());
		((PopulationImpl)scenario.getPopulation()).addAlgorithm(new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalc, timeCostCalc, new AStarLandmarksFactory(scenario.getNetwork(), timeCostCalc), routeFactory));
		((PopulationImpl)scenario.getPopulation()).runAlgorithms();
		
		EventsManager events = EventsUtils.createEventsManager();
		ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
		QSim qSim = new QSim(scenario, events);
		if (scenario.getConfig().scenario().isUseSignalSystems()){
			AdaptiveControllHead adaptiveControllHead = new AdaptiveControllHead();
			CarsOnLaneHandler carsOnLaneHandler = new CarsOnLaneHandler();
			carsOnLaneHandler.setAdaptiveControllHead(adaptiveControllHead);
			JbSignalBuilder jbBuilder = new JbSignalBuilder(scenario.getScenarioElement(SignalsData.class), new FromDataBuilder(scenario.getScenarioElement(SignalsData.class), events), carsOnLaneHandler, adaptiveControllHead);
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
