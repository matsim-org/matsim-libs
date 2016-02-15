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

import org.matsim.api.core.v01.Coord;
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
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ComputeMaxDCScorePlanAlgo implements PlanAlgorithm {
	
	private final String type;
	private final ActivityFacilityWithIndex[] typedFacilities;
	private final DestinationScoring scorer;
	private final DestinationSampler sampler;
	private final DestinationChoiceBestResponseContext lcContext;
	private final Id<Link> dummyLinkId = Id.createLinkId(1);
	private final DummyActivity dummyActivity = new DummyActivity(this.dummyLinkId); 
	
	public ComputeMaxDCScorePlanAlgo(final String type, final ActivityFacilityWithIndex[] typedFacilities,
			final DestinationScoring scorer, final DestinationSampler sampler, final DestinationChoiceBestResponseContext lcContext) {		
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
			
					for (ActivityFacilityWithIndex f : this.typedFacilities) {
						//check if facility is sampled
						int facilityIndex = f.getArrayIndex();
						if (!this.sampler.sample(facilityIndex, personIndex)) continue;
						
//						ActivityImpl act = new ActivityImpl(type, this.dummyLinkId);
//						act.setFacilityId(f.getId());
						this.dummyActivity.setType(type);
						this.dummyActivity.setFacilityId(f.getId());
						
						double epsilonScaleFactor = 1.0; // no scaling back needed here anymore
//						double dcScore = scorer.getDestinationScore(act, epsilonScaleFactor, activityIndex, p.getId());
						double dcScore = scorer.getDestinationScore(this.dummyActivity, epsilonScaleFactor, activityIndex, p.getId());
										
						if (dcScore > maxDCScore) {
							maxDCScore = dcScore;
						}
					}
				}
			}
		}
		p.getCustomAttributes().put(this.type, maxDCScore);	
	}
	
	/*
	 * Creating new ActivityImpl objects over and over again in the run(...) method is very expensive compared to the
	 * other stuff performed in that method (seems that "type.intern()", which is called in the constructor, is the problem). 
	 * The object is passed over to scorer.getDestinationScore(...) where only its type and facilityId are required. 
	 * Therefore, we create a DummyActivity object and re-use the existing string object from the input activity.
	 * 
	 * Tested this with a sample of 6.9k agents and 2.3m facilities and found a computation time reduction of 30%.
	 * 
	 * cdobler, oct'15
	 */
	private static final class DummyActivity implements Activity {

		private String type = null;
		private Id<ActivityFacility> facilityId = null;
		private final Id<Link> linkId;
		
		public DummyActivity(Id<Link> linkId) { this.linkId = linkId; }
		
		@Override
		public double getEndTime() { return 0; }

		@Override
		public void setEndTime(double seconds) { }

		@Override
		public String getType() { return this.type; }

		@Override
		public void setType(String type) { this.type = type; }

		@Override
		public Coord getCoord() { return null; }

		@Override
		public double getStartTime() { return 0; }

		@Override
		public void setStartTime(double seconds) { }

		@Override
		public double getMaximumDuration() { return 0; }

		@Override
		public void setMaximumDuration(double seconds) { }

		@Override
		public Id<Link> getLinkId() { return this.linkId; }

		@Override
		public Id<ActivityFacility> getFacilityId() { return this.facilityId; }
		
		public void setFacilityId(Id<ActivityFacility> facilityId) { this.facilityId = facilityId; }

		@Override
		public void setLinkId(Id<Link> id) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}	
	}
}