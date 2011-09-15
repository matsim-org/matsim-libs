/* *********************************************************************** *
 * project: org.matsim.*
 * Controller2D.java
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
package playground.gregor.sim2d_v2.controller;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v2.simulation.HybridQ2DMobsimFactory;
import playground.gregor.sim2d_v2.trafficmonitoring.MSATravelTimeCalculatorFactory;

@Deprecated // should not be derived from Controler
public class Controller2D extends Controler {

	protected Sim2DConfigGroup sim2dConfig;

	public Controller2D(String[] args) {
		super(args[0]);
		setOverwriteFiles(true);
		this.config.addQSimConfigGroup(new QSimConfigGroup());
		this.config.getQSimConfigGroup().setEndTime( 9*3600 + 20* 60);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		this.addMobsimFactory("hybridQ2D",new HybridQ2DMobsimFactory());
	}

	public Controller2D(Scenario sc) {
		super(sc);
		setOverwriteFiles(true);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		this.addMobsimFactory("hybridQ2D",new HybridQ2DMobsimFactory());
	}

	@Override
	protected void loadData() {
		super.loadData();
		initSim2DConfigGroup();
		ScenarioLoader2DImpl loader = new ScenarioLoader2DImpl(this.scenarioData);
		loader.load2DScenario();
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(
			PersonalizableTravelCost travelCosts,
			PersonalizableTravelTime travelTimes) {
		PlansCalcRoute a = new PlansCalcRoute(config.plansCalcRoute(), network, travelCosts, travelTimes, getLeastCostPathCalculatorFactory(), ((PopulationFactoryImpl) this.scenarioData.getPopulation().getFactory()).getModeRouteFactory());
		a.addLegHandler("walk2d", new NetworkLegRouter(this.network, a.getLeastCostPathCalculator(), a.getRouteFactory()));
		return a;
	}

	/**
	 * 
	 */
	private void initSim2DConfigGroup() {
		Module module = this.config.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		this.sim2dConfig = s;
		this.config.getModules().put("sim2d", s);
	}

	public static void main(String[] args) {

		String configFile = args[0];
		Config c = ConfigUtils.loadConfig(configFile);
		c.addQSimConfigGroup(new QSimConfigGroup());
		c.getQSimConfigGroup().setEndTime( 9*3600 + 20* 60);

		Scenario sc = ScenarioUtils.createScenario(c);
		((PopulationFactoryImpl)sc.getPopulation().getFactory()).setRouteFactory("walk2d", new LinkNetworkRouteFactory());
		ScenarioUtils.loadScenario(sc);

		Controler controller = new Controller2D(sc);
		controller.run();

	}

	public Sim2DConfigGroup getSim2dConfig() {
		return this.sim2dConfig;
	}

}
