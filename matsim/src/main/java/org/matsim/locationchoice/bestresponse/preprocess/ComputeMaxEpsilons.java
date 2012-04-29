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

package org.matsim.locationchoice.bestresponse.preprocess;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.locationchoice.bestresponse.DestinationSampler;
import org.matsim.locationchoice.bestresponse.scoring.DestinationChoiceScoring;
import org.matsim.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.locationchoice.utils.ActTypeConverter;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class ComputeMaxEpsilons extends AbstractMultithreadedModule {	
	private ScenarioImpl scenario;	
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private Config config;
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
	private static final Logger log = Logger.getLogger(ComputeMaxEpsilons.class);
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	private DestinationSampler sampler;
		
	public ComputeMaxEpsilons(ScenarioImpl scenario, String type, Config config, 
			ObjectAttributes facilitiesKValues, ObjectAttributes personsKValues, ScaleEpsilon scaleEpsilon,
			ActTypeConverter actTypeConverter,
			DestinationSampler sampler) {
		super(config.global().getNumberOfThreads());
		this.scenario = scenario;
		this.type = type;
		this.actTypeConverter = actTypeConverter;
		this.typedFacilities = this.scenario.getActivityFacilities().getFacilitiesForActivityType(
				actTypeConverter.convertType(type));
		if (this.typedFacilities.size() == 0) {
			log.warn("There are no facilities for type : " + type);
		}
		this.config = config; 
		this.facilitiesKValues = facilitiesKValues;
		this.personsKValues = personsKValues;
		this.scaleEpsilon = scaleEpsilon;
		this.sampler = sampler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		DestinationChoiceScoring scorer = new DestinationChoiceScoring(this.scenario.getActivityFacilities(), config, 
				this.facilitiesKValues, this.personsKValues, this.scaleEpsilon);
		return new EpsilonComputer(
				this.scenario, this.type, typedFacilities, scorer, this.scaleEpsilon, this.actTypeConverter, this.sampler);
	}
}
