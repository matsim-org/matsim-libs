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

package org.matsim.contrib.locationchoice.bestresponse.preprocess;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.LocationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DestinationChoiceScoring;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ComputeMaxDCScoreMultiThreatedModule extends AbstractMultithreadedModule {	
	private ScenarioImpl scenario;	
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private LocationChoiceBestResponseContext lcContext;
	private static final Logger log = Logger.getLogger(ComputeMaxDCScoreMultiThreatedModule.class);
	private DestinationSampler sampler;
		
	public ComputeMaxDCScoreMultiThreatedModule(ScenarioImpl scenario, String type, Config config, 
			LocationChoiceBestResponseContext lcContext, DestinationSampler sampler) {
		super(config.global().getNumberOfThreads());
		this.scenario = scenario;
		this.type = type;
		this.lcContext = lcContext;
		this.typedFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType(lcContext.getConverter().convertType(type));
		if (this.typedFacilities.size() == 0) {
			log.warn("There are no facilities for type : " + type);
		} 
		this.sampler = sampler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		DestinationChoiceScoring scorer = new DestinationChoiceScoring(this.lcContext);
		
		return new ComputeMaxDCScorePlanAlgo(
				this.scenario, this.type, typedFacilities, scorer, this.lcContext.getScaleEpsilon(),
				this.lcContext.getConverter(),
				this.sampler);
	}
}
