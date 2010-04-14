/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatX12Initialiser.java
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

package playground.mfeil.MDSAM;

import java.util.List;

import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;




/**
 * @author Matthias Feil
 * Initializes the PlansVariator replanning module.
 */

public class PlansVariatorInitializer extends AbstractMultithreadedModule{
	
	private final Controler controler;
	private final NetworkImpl 						network;
	private final DepartureDelayAverageCalculator 	tDepDelayCalc;
	private final LocationMutatorwChoiceSet locator;
	private final PlansCalcRoute router;
	private final List<String> actTypes;
	
	public PlansVariatorInitializer (Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler;
		this.network = controler.getNetwork();
		this.init(network);	
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network, controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
		this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler, controler.getScenario().getKnowledges());
		this.router = new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.createTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		ActivityTypeFinder finder = new ActivityTypeFinder (this.controler);
		finder.run(controler.getFacilities());
		this.actTypes = finder.getActTypes();
	}
	
	private void init(final NetworkImpl network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansVariator (this.controler, this.tDepDelayCalc, this.locator, this.router, this.actTypes);
	}
}
