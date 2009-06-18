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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.controler.Controler;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.population.algorithms.PlanAlgorithm;
import java.util.ArrayList;
import java.util.List;
import org.matsim.core.api.facilities.ActivityOption;



/**
 * @author Matthias Feil
 * Initializes the PlansVariator replanning module.
 */

public class PlansVariatorInitializer extends AbstractMultithreadedModule{
	
	private final Controler controler;
	private final LocationMutatorwChoiceSet locator;
	private final PlansCalcRoute router;
	private final List<String> actTypes;
	
	public PlansVariatorInitializer (Controler controler) {
		this.controler = controler;
		this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler, ((ScenarioImpl)controler.getScenarioData()).getKnowledges());
		this.router = new PlansCalcRoute (controler.getConfig().plansCalcRoute(), controler.getNetwork(), controler.getTravelCostCalculator(), controler.getTravelTimeCalculator(), controler.getLeastCostPathCalculatorFactory());
		ActivityOptionFinder finder = new ActivityOptionFinder ();
		finder.run(controler.getFacilities());
		this.actTypes = finder.getActTypes();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansVariator (this.controler, this.locator, this.router, this.actTypes);
	}
}
