/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelMinDisutility;

public class AStarLandmarksTask implements ScenarioSinkSourceLeastCostPathCalculator {

	private AStarLandmarksFactory factory;
	
	private ScenarioSink sink;

	public AStarLandmarksTask(AStarLandmarksFactory factory,
			TravelMinDisutility travelMinCost) {
		super();
		this.factory = factory;
		this.travelMinCost = travelMinCost;
	}

	private TravelMinDisutility travelMinCost;

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	@Override
	public void initialize(Scenario scenario) {
		factory.processNetwork(scenario.getNetwork(), travelMinCost, scenario.getConfig().global().getNumberOfThreads());
		sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		sink.process(scenario);
	}

	@Override
	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return factory;
	}

}
