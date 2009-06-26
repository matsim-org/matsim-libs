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

import org.matsim.core.api.ScenarioImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.mfeil.MDSAM.ActivityTypeFinder;



/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to initialise the PlanomatX module.
 */

public class PlanomatX12Initialiser extends AbstractMultithreadedModule{
	
	
	private final NetworkLayer 						network;
	private final Controler							controler;
	private final PreProcessLandmarks				preProcessRoutingData;
	private final LocationMutatorwChoiceSet 		locator;
	private /*final*/ DepartureDelayAverageCalculator 	tDepDelayCalc;
	private final ActivityTypeFinder 				finder;
	
	
	public PlanomatX12Initialiser (final ControlerMFeil controler, ActivityTypeFinder finder) {
		
		this.preProcessRoutingData 	= new PreProcessLandmarks(new FreespeedTravelTimeCost());
		this.preProcessRoutingData.run(controler.getNetwork());
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);	
		this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler, ((ScenarioImpl)controler.getScenarioData()).getKnowledges());
		
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network,controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
		this.finder = finder;
	}
	
	public PlanomatX12Initialiser (final ControlerMFeil controler, 
			final PreProcessLandmarks preProcessRoutingData,
			final LocationMutatorwChoiceSet locator, ActivityTypeFinder finder) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);
		this.preProcessRoutingData = preProcessRoutingData;	
		this.locator = locator;
		this.finder = finder;
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm planomatXAlgorithm;
		planomatXAlgorithm = new PlanomatX18 (this.controler, this.preProcessRoutingData, this.locator, this.tDepDelayCalc, this.finder);
		return planomatXAlgorithm;
	}
	
	// TODO The list of valid act types across all agents must be implemented here.
	/*private ArrayList<Activity> findActTypes (){
		Facilities facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE,null);
		return new ArrayList<Activity>();
	}*/
}
