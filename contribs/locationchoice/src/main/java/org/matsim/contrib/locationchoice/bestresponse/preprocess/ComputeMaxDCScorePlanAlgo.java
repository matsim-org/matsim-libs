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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext.ActivityFacilityWithIndex;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.scoring.DestinationScoring;
import org.matsim.core.population.ActivityImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ComputeMaxDCScorePlanAlgo implements PlanAlgorithm {
	private String type;
	private TreeMap<Id<ActivityFacility>, ActivityFacilityWithIndex> typedFacilities;
	private DestinationScoring scorer;
	private DestinationSampler sampler;
	private DestinationChoiceBestResponseContext lcContext;
	private final Id<Link> dummyLinkId = Id.createLinkId(1);
	
	public ComputeMaxDCScorePlanAlgo(String type, TreeMap<Id<ActivityFacility>, ActivityFacilityWithIndex> typedFacilities,
			DestinationScoring scorer, DestinationSampler sampler, DestinationChoiceBestResponseContext lcContext) {		
		this.type = type;
		this.typedFacilities = typedFacilities;
		this.scorer = scorer;
		this.sampler = sampler;
		this.lcContext = lcContext;
	}
		
	@Override
	public void run(Plan plan) {
		Person p = plan.getPerson();
		int personIndex = this.lcContext.getPersonIndex(p.getId());
		double maxDCScore = 0.0;		
		/*
		 * Find the max dc score of all activities of this.type.
		 * Different activities in a plan have now different epsilons!
		 * TODO: Future improvement: store max dc score *per* activity -> computational gain 
		 */			
		int activityIndex = -1 ;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				activityIndex++ ;
				if (this.lcContext.getConverter().convertType(((Activity) pe).getType()).equals(type)) {
			
					for (ActivityFacilityWithIndex f : typedFacilities.values()) {
						//check if facility is sampled
						int facilityIndex = f.getArrayIndex();
						if (!this.sampler.sample(facilityIndex, personIndex)) continue;
						
						ActivityImpl act = new ActivityImpl(type, this.dummyLinkId);
						act.setFacilityId(f.getId());
						
						double epsilonScaleFactor = 1.0; // no scaling back needed here anymore
						double dcScore = scorer.getDestinationScore(act, epsilonScaleFactor, activityIndex, p.getId());
										
						if (dcScore > maxDCScore) {
							maxDCScore = dcScore;
						}
					}
				}
			}
		}
		p.getCustomAttributes().put(this.type, maxDCScore);	
	}
}