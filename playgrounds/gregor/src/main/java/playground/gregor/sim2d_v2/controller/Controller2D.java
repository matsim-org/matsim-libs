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

import org.matsim.core.config.Module;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.gregor.pedvis.PedVisPeekABot;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.ScenarioLoader2DImpl;
import playground.gregor.sim2d_v2.simulation.HybridQ2DMobsimFactory;
import playground.gregor.sims.msa.MSATravelTimeCalculatorFactory;

@Deprecated // should not be derived from Controler
public class Controller2D extends Controler {

	private PedVisPeekABot vis;
	protected Sim2DConfigGroup sim2dConfig;
	private ScenarioLoader2DImpl loader;

	public Controller2D(String[] args) {
		super(args[0]);
		setOverwriteFiles(true);
		this.config.addQSimConfigGroup(new QSimConfigGroup());
		this.config.getQSimConfigGroup().setEndTime( 9*3600 + 20* 60);
		setTravelTimeCalculatorFactory(new MSATravelTimeCalculatorFactory());
		this.addMobsimFactory("hybridQ2D",new HybridQ2DMobsimFactory());
	}

	@Override
	protected void loadData() {
		//TODO remove this method!!
		if (!this.scenarioLoaded) {
			initSim2DConfigGroup();
			this.loader = new ScenarioLoader2DImpl(this.scenarioData);
			this.loader.loadScenario();
			this.network = (NetworkImpl) this.loader.getScenario().getNetwork();
			this.population = this.loader.getScenario().getPopulation();
			this.scenarioLoaded = true;

		}

	}



	@Override
	public PlanAlgorithm createRoutingAlgorithm(
			PersonalizableTravelCost travelCosts,
			PersonalizableTravelTime travelTimes) {
		PlansCalcRoute a = (PlansCalcRoute) super.createRoutingAlgorithm(travelCosts, travelTimes);
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
		Controler controller = new Controller2D(args);
		controller.run();

	}

	public Sim2DConfigGroup getSim2dConfig() {
		return this.sim2dConfig;
	}

}
