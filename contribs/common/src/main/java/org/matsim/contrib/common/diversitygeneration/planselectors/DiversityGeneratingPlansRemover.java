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
package org.matsim.contrib.common.diversitygeneration.planselectors;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.selectors.AbstractPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.PtConstants;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The idea, as the name says, is to not remove the worst plan, but remove plans such that diversity is maintained.
 * The design idea stems from path size logit, which reduces the probability to select alternatives which are similar
 * to other alternatives by adding a penalty to their utility.  
 * <p></p>
 * So if you have, for example, 4 similar plans with high scores and one other plan with a lower score, then the scores of 
 * the similar plans are artificially reduced, and one of them will in consequence be removed. 
 * In the present incarnation, we make sure that not the best of those 4 will be removed.
 * <p></p>
 * Note that once all similar plans are removed, the remaining best plan will not be similar to any other plan any more, and
 * thus no longer incur a similarity penalty.  So it will never be removed.
 * <p></p>
 * This class has <i>not</i> yet been extensively tested and so it is not clear if it contains bugs, how well it works, or if parameters
 * should be set differently.  If someone wants to experiment, the class presumably should be made configurable (or copied before 
 * modification). 
 * <p></p>
 * There is also material in playground.vsp .
 * <p></p>
 * There are also some hints to literature at {@link PopulationUtils#calculateSimilarity}
 * 
 * @author nagel, ikaddoura
 */
public final class DiversityGeneratingPlansRemover extends AbstractPlanSelector {
	// According to what I have tried (with equil and pt mode choice), the penalties should rather be somewhere
	// between 0.1 and 0.3.  1 already is too much; what essentially happens is that agents end up spending
	// too much time on evaluating mutations of plans that already have a low base score, but remain in the
	// choice set because they are so different.  One should re-run this with one of the "reserved space"
	// examples (airplanes; evacuation shelters). kai, aug'18

	public static final class Builder implements Provider<PlanSelector<Plan, Person>> {

		private Network network;
		private double actTypeWeight = 0.1;
		private double locationWeight = 0.3;
		private double actTimeParameter = 0.3;
		private double sameRoutePenalty = 0.3;
		private double sameModePenalty = 0.3;

		private StageActivityTypes stageActivities = new StageActivityTypes() {
			@Override
			public boolean isStageActivity(String activityType) {
				return activityType.equals(PtConstants.TRANSIT_ACTIVITY_TYPE);
			}
		};

		@Inject final Builder setNetwork(Network network) {
			this.network = network;
			return this ;
		}
		public final Builder setSameActivityTypePenalty( double val ) {
			this.actTypeWeight = val ;
			return this ;
		}
		public final Builder setSameLocationPenalty( double val ) {
			this.locationWeight = val ;
			return this ;
		}
		public final Builder setSameActivityEndTimePenalty( double val) {
			this.actTimeParameter = val ;
			return this ;
		}
		public final Builder setSameRoutePenalty( double val) {
			this.sameRoutePenalty = val;
			return this ;
		}
		public final Builder setSameModePenalty( double val) {
			this.sameModePenalty = val;
			return this ;
		}
		public final Builder setStageActivityTypes( StageActivityTypes val) {
			this.stageActivities = val;
			return this ;
		}
		@Override
		public final DiversityGeneratingPlansRemover get() {
			return new DiversityGeneratingPlansRemover(
					this.network,
					this.actTypeWeight,
					this.locationWeight,
					this.actTimeParameter,
					this.sameRoutePenalty,
					this.sameModePenalty,
					this.stageActivities);
		}
	}

	private DiversityGeneratingPlansRemover(Network network,
			double actTypeWeight, double locationWeight,
			double actTimeParameter, double sameRoutePenalty,
			double sameModePenalty, StageActivityTypes stageActivities) {
		this.network = network;
		this.actTypeWeight = actTypeWeight;
		this.locationWeight = locationWeight;
		this.actTimeWeight = actTimeParameter;
		this.sameRoutePenalty = sameRoutePenalty;
		this.sameModePenalty = sameModePenalty;
		this.stageActivities = stageActivities;
	}

	static private final Logger log = Logger.getLogger(DiversityGeneratingPlansRemover.class);

	private final Network network;

	private final double actTypeWeight;
	private final double locationWeight;
	private final double actTimeWeight;

	private final double sameRoutePenalty;
	private final double sameModePenalty;
	private final StageActivityTypes stageActivities;

	@Override
	protected final Map<Plan, Double> calcWeights(List<? extends Plan> plans) {
		if ( plans.isEmpty() ) {
			throw new RuntimeException("empty plans set; this will not work ...") ;
		}

		Map<Plan,Double> map = new HashMap<Plan,Double>() ;

		double[] utils = new double[plans.size()] ;

		// --- initialize utils: ---
		int pp=0 ;
		for ( Plan plan : plans ) {
			utils[pp] = plan.getScore() ;
			if ( Double.isNaN(utils[pp]) ) {
				log.warn( "utils is NaN; id: "  + plan.getPerson().getId());
			}
			pp++ ;
		}

		int rr=0 ;
		for ( Plan plan1 : plans ) {
//			log.info( "rr=" + rr + "; utils=" + utils[rr]) ;
			for ( Plan plan2 : plans ) {
				// yyyy there is really no need to compare the plan with itself.  kai/johan, mar'14, should not happen anymore... ihab, may'14
				if (plan1 == plan2) {
					// same plan
				} else {
					utils[rr] -= similarity( plan1, plan2 ) ;
//					log.info( "rr=" + rr + "; utils=" + utils[rr]) ;

					if ( Double.isNaN(utils[rr]) ) {
						log.warn( "utils is NaN; id: " + plan1.getPerson().getId() ) ;
					}
				}	
			}
//			for ( PlanElement pe : plan1.getPlanElements() ) {
//				log.info( pe.toString() ) ;
//			}
//			log.info("") ;
			rr++ ;
		}

		//		// --- calculate expSum: ---
		//		double expSum = 0. ;
		//		for ( int ii=0 ; ii<utils.length ; ii++ ) {
		//			expSum += Math.exp( utils[ii] ) ;
		//		}
		//
		//		// --- calculate weights
		//		int qq=0 ;
		//		for ( Plan plan : plans ) {
		//			double weight = Math.exp( utils[qq] ) / expSum ;
		//			map.put( plan,  weight ) ;
		//		}

		// start with an exact version: for the time being, we do not want that the best plan vanishes.
		// The worst plan (taking into account the penalty for similarity!) will be removed.
		// Alternative (Ihab): Remove the best plan from the evaluation; apply algo only to other plans. may'14
		double minUtil = Double.POSITIVE_INFINITY ;
		Integer minIdx = null ;
		for ( int kk = 0 ; kk<utils.length ; kk++ ) {
			if ( utils[kk] < minUtil ) {
				minUtil = utils[kk] ;
				minIdx = kk ;
			}
		}
		if ( minIdx==null ) {
			log.warn("minIdx is still null; there is a problem somehwere.") ;
			for ( int kk=0 ; kk<utils.length; kk++ ) {
				log.warn( "kk: " + kk + "; utils: " + utils[kk] ) ;
			}
		}

		int ab = 0 ;
		for ( Plan plan : plans ) {
			if ( ab==minIdx){
				map.put( plan, 1. ) ;
			} else {
				map.put( plan, 0. ) ;
			}
			ab++ ;
		}


		return map ;
	}

	/* package-private, for testing */ double similarity( Plan plan1, Plan plan2 ) {
		double simil = 0. ;
		{
			List<Activity> activities1 = TripStructureUtils.getActivities(plan1, stageActivities) ;
			List<Activity> activities2 = TripStructureUtils.getActivities(plan2, stageActivities) ;
			simil += PopulationUtils.calculateSimilarity(activities1, activities2, actTypeWeight, locationWeight, actTimeWeight ) ;
			if ( Double.isNaN(simil) ) {
				log.warn("simil is NaN; id: " + plan1.getPerson().getId() ) ;
			}
		}
		{
			List<Leg> legs1 = TripStructureUtils.getLegs(plan1 ) ;
			List<Leg> legs2 = TripStructureUtils.getLegs(plan2 ) ;
			simil += PopulationUtils.calculateSimilarity(legs1, legs2, network, this.sameModePenalty, this.sameRoutePenalty ) ;
			if ( Double.isNaN(simil) ) {
				log.warn("simil is NaN; id: " + plan1.getPerson().getId() ) ;
			}
		}		

		return simil ;
	}

}
