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

package playground.vsp.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;

public class TravelCostCalculatorTask implements ScenarioSinkSource {

	private ScenarioSink sink;
	
	private PlanCalcScoreConfigGroup group;

	private TravelTimeCalculatorTask travelTimeCalculator;

	private TravelDisutilityFactory travelCostCalculatorFactory;

	private TravelDisutility travelCostCalculator;
	
	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	@Override
	public void initialize(Scenario scenario) {
		this.sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		travelCostCalculator = travelCostCalculatorFactory.createTravelDisutility(travelTimeCalculator.getTravelTimeCalculator(), group);	
		sink.process(scenario);
	}

	public TravelCostCalculatorTask(TravelDisutilityFactory travelCostCalculatorFactory, PlanCalcScoreConfigGroup group) {
		super();
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
		this.group = group;
	}

	TravelDisutility getTravelCostCalculator() {
		return travelCostCalculator;
	}

	void setTravelTimeCalculator(TravelTimeCalculatorTask travelTimeCalculator) {
		this.travelTimeCalculator = travelTimeCalculator;
	}

}
