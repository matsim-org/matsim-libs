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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.locationchoice.bestresponse.DestinationSampler;
import org.matsim.locationchoice.bestresponse.scoring.DestinationChoiceScoring;
import org.matsim.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.locationchoice.utils.ActTypeConverter;
import org.matsim.population.algorithms.PlanAlgorithm;

public class EpsilonComputer implements PlanAlgorithm {
	private String type;
	private TreeMap<Id, ActivityFacility> typedFacilities;
	private DestinationChoiceScoring scorer;
	private ScaleEpsilon scaleEpsilon;
	private ActTypeConverter actTypeConverter;
	private DestinationSampler sampler;
			
	public EpsilonComputer(ScenarioImpl scenario, String type, TreeMap<Id, ActivityFacility> typedFacilities,
			DestinationChoiceScoring scorer, ScaleEpsilon scaleEpsilon, ActTypeConverter actTypeConverter,
			DestinationSampler sampler) {		
		this.type = type;
		this.typedFacilities = typedFacilities;
		this.scorer = scorer;
		this.scaleEpsilon = scaleEpsilon;
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
				double epsilon = scorer.getDestinationScore((PlanImpl)p.getSelectedPlan(), act);
				
				// scale back epsilons
				double scale = this.scaleEpsilon.getEpsilonFactor(act.getType());
				epsilon /= scale;
				
				if (epsilon > maxEpsilon) {
					maxEpsilon = epsilon;
				}
			}
		}
		// temporarily store maxEpsilon here: (s_l)
		if (((PersonImpl)p).getDesires() == null) ((PersonImpl)p).createDesires("");
		((PersonImpl)p).getDesires().setDesc(((PersonImpl)p).getDesires().getDesc() + maxEpsilon + "_");	
	}
}