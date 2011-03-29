/* *********************************************************************** *
 * project: org.matsim.*
 * JointControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.run;

import org.apache.log4j.Logger;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.replanning.JointPlansReplanning;
import playground.thibautd.jointtripsoptimizer.replanning.JointStrategyManager;
import playground.thibautd.jointtripsoptimizer.scoring.JointPlansScoring;

/**
 * Custom controler for handling clique replanning
 * @author thibautd
 */
public class JointControler extends Controler {
	private static final Logger log =
		Logger.getLogger(JointControler.class);


	/**
	 * replacement for the private super() fields.
	 */
	//private PlansScoring plansScoring = null;
	private JointPlansScoring plansScoring = null;
	//private RoadPricing roadPricing = null;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */

	/**
	 * Only constructor available, to enforce the initialization of a
	 * ScenarioWithCliques in the controler.
	 * The config has to be set in the scenario before.
	 */
	public JointControler(ScenarioWithCliques scenario) {
		super((ScenarioImpl) scenario);
	}

	/*
	 * =========================================================================
	 * overrided methods
	 * =========================================================================
	 */
	/**
	 * Same as the loadCoreListeners of the base class, excepts that it loads a
	 * JointPlanReplanning instance instead of a PlansReplanning one.
	 * This allows handling PopulationWithCliques populations.
	 * This has the drawback of breaking the getRoadPricing (final) method of
	 * the controler.
	 * {@inheritDoc}
	 * @see Controler#loadCoreListeners()
	 */
	@Override
	protected void loadCoreListeners() {
		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important. Example: The
		 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
		 * turn is used by the PlansScoring-Listener. Note that the execution
		 * order is contrary to the order the listeners are added to the list.
		 */

		this.addCoreControlerListener(new CoreControlerListener());

		// the default handling of plans
		this.plansScoring = new JointPlansScoring();
		//this.plansScoring = new PlansScoring();
		this.addCoreControlerListener(this.plansScoring);

		// load road pricing, if requested
		if (this.config.scenario().isUseRoadpricing()) {
			//this.roadPricing = new RoadPricing(); //XXX roadProcing is private!
			//this.addCoreControlerListener(this.roadPricing);
			this.addCoreControlerListener(new RoadPricing());
			log.warn("RoadPricing set in JointControler: getRoadPricing will be"
					+" broken.");
		}

		this.addCoreControlerListener(new JointPlansReplanning());
		this.addCoreControlerListener(new PlansDumping());

		this.addCoreControlerListener(new EventsHandling(this.events));
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new JointStrategyManager();
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	@Override
	public PlansScoring getPlansScoring() {
		return this.plansScoring;
	}

}
