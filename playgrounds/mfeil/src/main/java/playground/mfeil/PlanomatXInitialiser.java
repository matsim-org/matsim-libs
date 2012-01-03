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
package playground.mfeil;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.locationchoice.timegeography.RecursiveLocationMutator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;




/**
 * @author Matthias Feil
 * Initiating PlanomatX.
 */

public class PlanomatXInitialiser extends AbstractMultithreadedModule{
	
	
	private final Network network;
	private final Controler								controler;
	private final RecursiveLocationMutator 			locator;
	private /*final*/ DepartureDelayAverageCalculator 	tDepDelayCalc;
	private final ActivityTypeFinder 					finder;
	
	
	public PlanomatXInitialiser (final Controler controler, ActivityTypeFinder finder) {
		super(controler.getConfig().global());
		this.network = controler.getNetwork();
		this.controler = controler;
	//	this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler, ((ScenarioImpl)controler.getScenario()).getKnowledges());
		this.locator = new LMwCSCustomized(controler.getNetwork(), controler, controler.getScenario().getKnowledges());
		
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network,controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
		this.finder = finder;
	}
	
	public PlanomatXInitialiser (final ControlerMFeil controler, 
			final RecursiveLocationMutator locator, ActivityTypeFinder finder) {
		super(controler.getConfig().global());
		this.network = controler.getNetwork();
		this.controler = controler;
		this.locator = locator;
		this.finder = finder;
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm planomatXAlgorithm;
		planomatXAlgorithm = new PlanomatX (this.controler, this.locator, this.tDepDelayCalc, this.finder);
		return planomatXAlgorithm;
	}
}
