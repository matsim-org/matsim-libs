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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DestinationScoring;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ComputeMaxDCScorePlanAlgo implements PlanAlgorithm {
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private DestinationScoring scorer;
	private ActTypeConverter actTypeConverter;
	private DestinationSampler sampler;
			
	public ComputeMaxDCScorePlanAlgo(ScenarioImpl scenario, String type, TreeMap<Id, ActivityFacility> typedFacilities,
			DestinationScoring scorer, ActTypeConverter actTypeConverter,
			DestinationSampler sampler) {		
		this.type = type;
		this.typedFacilities = typedFacilities;
		this.scorer = scorer;
		this.actTypeConverter = actTypeConverter;
		this.sampler = sampler;
	}
		
	@Override
	public void run(Plan plan) {
		Person p = plan.getPerson();
		//ceck if plan contains activity of type
		boolean typeInPlan = false;
		for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Activity) {
				if (this.actTypeConverter.convertType(((Activity) pe).getType()).equals(type)) typeInPlan = true;
			}
		}
		double maxEpsilon = 0.0;
		if (typeInPlan) {
			for (Facility f : typedFacilities.values()) {
				//check if facility is sampled
				if (!this.sampler.sample(f.getId(), plan.getPerson().getId())) continue;
				
				ActivityImpl act = new ActivityImpl(type, new IdImpl(1));
				act.setFacilityId(f.getId());
				
				double epsilonScaleFactor = 1.0; // no scaling back needed here anymore
				double epsilon = scorer.getDestinationScore((PlanImpl)p.getSelectedPlan(), act, epsilonScaleFactor);
								
				if (epsilon > maxEpsilon) {
					maxEpsilon = epsilon;
				}
			}
		}
		// temporarily store maxEpsilon here: (s_l)
		// TODO: get rid of that hack. ah feb'13
		// use customizable
		if (((PersonImpl)p).getDesires() == null) ((PersonImpl)p).createDesires("");
		((PersonImpl)p).getDesires().setDesc(((PersonImpl)p).getDesires().getDesc() + maxEpsilon + "_");	
	}
}