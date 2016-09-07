/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.vsp.planselectors;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.AbstractPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.PtConstants;

/**
 * This class aims to increase the diversity of plans in an agent's choice set. This is achieved by removing a plan once
 * it is found to be <i>similar</i> to another plan of the choice set. If no plan is considered <i>similar</i> to another plan
 * the standard MATSim behavior is preserved, i.e. as of May 2014 the worst scored plan will be removed from the choice set.
 * As first tests indicate, this effectively prevents agents from over-adapting, i.e. arriving at a stop only seconds before
 * the bus departs.
 * <p>
 * Details:<br>
 * A plan is considered <i>similar</i> to another plan if all of the similarity checkers consider the plan <i>similar</i>.
 * For example, two plans with the same activity end times are <i>similar</i> if they also use the same mode of transport.
 * If one of the similarity checkers fails, the plans are considered <i>dissimilar</i>. Note that currently only activity
 * end times are checked for. See {@link DiversityGeneratingPlansRemoverANIK#similarity(Plan, Plan, StageActivityTypes, double)}
 * for information on adding further similarity checkers. From two plans considered as <i>similar</i> the older one is preserved
 * and the newer one will be deleted. The comparison stops at this point and further plans are not checked for. Note that this
 * class can only delete one plan at a time. Multiple iterations are required in case more than two plans would be considered
 * as <i>similar</i>. Note that the similarity checks depend on the plans of the choice set and may thus yield different results
 * once a plan is removed. Further note that the order in which plans are compared to each other depends on the List implementation
 * in which the plans are stored. As of May 2014 this is an ArrayList ({@link Person#plans}).
 * 
 * @author aneumann
 * @author ikaddoura
 */
public final class DiversityGeneratingPlansRemoverANIK extends AbstractPlanSelector {
	
	private final StageActivityTypes stageActivities;
	private final double similarTimeInterval;
	
	/**
	 * Private - use the {@link DiversityGeneratingPlansRemoverANIK.Builder} instead.
	 */
	private DiversityGeneratingPlansRemoverANIK(Network network, double similarTimeInterval, StageActivityTypes stageActivities) {
		this.similarTimeInterval = similarTimeInterval;
		this.stageActivities = stageActivities;
	}

	public static final class Builder {
		// Defining default values
		private double similarTimeInterval = 5.0 * 60.;
		
		private StageActivityTypes stageActivities = new StageActivityTypes() {
			@Override
			public boolean isStageActivity(String activityType) {
				return activityType.equals(PtConstants.TRANSIT_ACTIVITY_TYPE);
			}
		};

		public final void setSimilarTimeInterval(double setSimilarTimeInterval) {
			this.similarTimeInterval = setSimilarTimeInterval;
		}

		public final void setStageActivityTypes(StageActivityTypes stageActivities) {
			this.stageActivities = stageActivities;
		}

		public final DiversityGeneratingPlansRemoverANIK build(Network network) {
			return new DiversityGeneratingPlansRemoverANIK(network,	this.similarTimeInterval, this.stageActivities);
		}
	}
	
	/**
	 * @return Map giving a weight for each plan. Higher weights have a higher probability of being selected.
	 */
	@Override
	protected Map<Plan, Double> calcWeights(List<? extends Plan> plans) {
		if ( plans.isEmpty() ) {
			throw new RuntimeException("empty plans set; this will not work ...") ;
		}
				
		Map<Plan,Double> map = new HashMap<Plan,Double>() ;
		// Initialize all plans with a weight of zero. 
		for (Plan plan : plans) {
			map.put(plan, 0.);
		}
		
		// Compare each plan with each other. If two plans are similar. The newer one gets dropped.
		for ( Plan plan1 : plans ) {
			for ( Plan plan2 : plans ) {
				
				if (plan1 == plan2){
					// same plan, those are definitely similar. So, ignore them.
				} else {
					// check two plans for similarity TODO Should be a kind of builder passed further down that configures all similarity checkers
					if (similarity( plan1, plan2, this.stageActivities, this.similarTimeInterval)) {
						// This one is similar. Tag it as to be removed and return. We only can remove one plan. So we remove the newer one.
						map.put(plan2, 1.);
						return map;
					} else {
						// Not similar. Proceed with the next plan.
					}
				}	
			}
		}
		
		// In case there is no similar plan, fall back to the standard behavior of MATSim (as of May, 2014). Remove worst plan from choice set.
		Plan plan = new WorstPlanForRemovalSelector().selectPlan(plans.get(0).getPerson());
		map.put(plan, 1.);
		
		return map ;
	}

	/**
	 * Compare two plans for similarity. A plan is considered similar to another plan, if all similarity checkers consider it being similar.<br>
	 * TODO The checkers should be implemented as individual classes being configured once with a builder passed to them.
	 * 
	 * @param plan1 First plan to be compared.
	 * @param plan2 Second plan to be compared.
	 * @param stageActivities Type of activities that should be ignored, e.g. {@link PtConstants#TRANSIT_ACTIVITY_TYPE}.
	 * @param similarTimeInterval The interval in which two activities are considered as being the same. TODO Builder implemenation
	 * @return <code>true</code> if both plans are considered similar, otherwise <code>false</code>.
	 */
	private boolean similarity( Plan plan1, Plan plan2, StageActivityTypes stageActivities, double similarTimeInterval) {
		
		// Check for the first dimension
		boolean similarTimes = checkActivityTimes(plan1, plan2, stageActivities, similarTimeInterval);
		
		// Further checks can be implemented the same way.
		// boolean similarModes = checkTransportModes(plan1, plan2);
		
		// 
		if (similarTimes /* && similarRoutes && similarModes */) {
			return true;
		} else {
			return false;
		}	
	}

	/**
	 * Compare two plans for similarity. A plan is considered similar to another plan, if all activities are within the specified interval.
	 * 
	 * @param plan1 First plan to be compared.
	 * @param plan2 Second plan to be compared.
	 * @param stageActivities Type of activities that should be ignored, e.g. {@link PtConstants#TRANSIT_ACTIVITY_TYPE}.
	 * @param similarTimeInterval The interval in which two activities are considered as being the same.
	 * @return <code>true</code> if both plans are considered similar, otherwise <code>false</code>.
	 */
	private boolean checkActivityTimes(Plan plan1, Plan plan2, StageActivityTypes stageActivities, double similarTimeInterval) {
		
		List<Activity> activities1 = TripStructureUtils.getActivities(plan1, stageActivities) ;
		List<Activity> activities2 = TripStructureUtils.getActivities(plan2, stageActivities) ;
		
		Iterator<Activity> it1 = activities1.iterator() ;
		Iterator<Activity> it2 = activities2.iterator() ;
		
		for ( ; it1.hasNext() && it2.hasNext() ; ) {
			Activity act1 = it1.next() ;
			Activity act2 = it2.next() ;
			
			if ( Double.isInfinite( act1.getEndTime() ) && Double.isInfinite( act2.getEndTime() ) ){
				// both activities have no end time, no need to compute a similarity penalty
			} else {
				// both activities have an end time, comparing the end times
				
				// Calculate the difference of both activities' end times.
				double delta = Math.abs(act1.getEndTime() - act2.getEndTime()) ;
				if (delta <= similarTimeInterval) {
					// This one is similar. Proceed with the next activity.
				} else {
					// Those two are not similar. Thus, the whole plan is considered as being not similar.
					return false;
				}
			}
		}
		// We never found two dissimilar activities. So, both plans are considered as being similar. 
		return true;
	}

}
