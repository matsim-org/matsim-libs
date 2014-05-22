/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.AbstractPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.PtConstants;

/**
 * The idea, as the name says, is to not remove the worst plan, but remove plans such that diversity is maintained.
 * The design idea stems from path size logit, which reduces the probability to select alternatives which are similar
 * to other alternatives by adding a penalty to their utility.  
 * <p/>
 * So if you have, for example, 4 similar plans with high scores and one other plan with a lower score, then the scores of 
 * the similar plans are artificially reduced, and one of them will in consequence be removed. 
 * In the present incarnation, we make sure that not the best of those 4 will be removed.
 * <p/>
 * Note that once all similar plans are removed, the remaining best plan will not be similar to any other plan any more, and
 * thus no longer incurr a similarity penalty.  So it will never be removed.
 * <p/>
 * This class has <i>not</i> yet been extensively tested and so it is not clear if it contains bugs, how well it works, or if parameters
 * should be set differently.  If someone wants to experiment, the class presumably should be made configurable (or copied before 
 * modification). 
 * 
 * @author nagel, ikaddoura, aneumann
 */
public final class DiversityGeneratingPlansRemoverANIK extends AbstractPlanSelector {
	
	public static final class Builder {
		// 2.5 min in both direction results in a time interval of 5 min
		private double similarTimeInterval = 2.5 * 60.;
		
		private StageActivityTypes stageActivities = new StageActivityTypes() {
			
			@Override
			public boolean isStageActivity(String activityType) {
				return activityType.equals(PtConstants.TRANSIT_ACTIVITY_TYPE);
			}
		};
	
		public final void setSimilarTimeInterval( double val) {
			this.similarTimeInterval = val ;
		}
		public final void setStageActivityTypes( StageActivityTypes val) {
			this.stageActivities = val;
		}
		
		public final DiversityGeneratingPlansRemoverANIK build( Network network ) {
			return new DiversityGeneratingPlansRemoverANIK(
					network,
					this.similarTimeInterval,
					this.stageActivities);
		}
	}
	
	private DiversityGeneratingPlansRemoverANIK(Network network,
			double similarTimeInterval,
			StageActivityTypes stageActivities) {
		this.network = network;
		this.similarTimeInterval = similarTimeInterval;
		this.stageActivities = stageActivities;
	}

	private final Network network;
	private final StageActivityTypes stageActivities;
	private final double similarTimeInterval;
	
	@Override
	protected Map<Plan, Double> calcWeights(List<? extends Plan> plans) {
		if ( plans.isEmpty() ) {
			throw new RuntimeException("empty plans set; this will not work ...") ;
		}
				
		Map<Plan,Double> map = new HashMap<Plan,Double>() ;
		for (Plan plan : plans) {
			map.put(plan, 0.);
		}
		
		for ( Plan plan1 : plans ) {
			
			for ( Plan plan2 : plans ) {
				
				if (plan1 == plan2){
					// same plan
				} else {
					if (similarity( plan1, plan2, stageActivities, similarTimeInterval, network)) {
						map.put(plan2, 1.);
						return map;
						
					} else {
						// proceed with the next plan
					}
				}	
			}
		}
		
		// in case there is no similar plan, fall back to standard behavior
		Plan plan = new WorstPlanForRemovalSelector().selectPlan(plans.get(0).getPerson());
		map.put(plan, 1.);
		
		return map ;
	}

	private boolean similarity( Plan plan1, Plan plan2, StageActivityTypes stageActivities, double similarTimeInterval, Network network ) {
		
		boolean similarTimes = checkActivityTimes(plan1, plan2, stageActivities, similarTimeInterval);
		// add further methods
		
		if (similarTimes /* && similarRoutes && similarModes */) {
			return true;
		} else {
			return false;
		}	
	}

	/**
	 * @param plan1
	 * @param plan2
	 * @param stageActivities 
	 * @param similarTimeInterval 
	 * @return
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
				
				double delta = Math.abs(act1.getEndTime() - act2.getEndTime()) ;
				if (delta <= similarTimeInterval) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}

}
