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
package playground.southafrica.kai.gauteng;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.selectors.AbstractPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

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
 * @author nagel
 */
public final class DiversityGeneratingPlansRemover extends AbstractPlanSelector {
	static private final Logger log = Logger.getLogger(DiversityGeneratingPlansRemover.class);
	
	private static final double sameActTypePenalty = 5;
	private static final double sameLocationPenalty = 5;
	private static final double sameRoutePenalty = 5;
	private static final double sameModePenalty = 5;
	private final Network network;
	
	public DiversityGeneratingPlansRemover( Network network ) {
		this.network = network ;
	}

	@Override
	protected Map<Plan, Double> calcWeights(List<? extends Plan> plans) {
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
				log.warn( "utils is NaN; id: "  + plan.getPerson().getId() ) ;
			}
			pp++ ;
		}

		int rr=0 ;
		for ( Plan plan1 : plans ) {
			for ( Plan plan2 : plans ) {
				// yyyy there is really no need to compare the plan with itself.  kai/johan, mar'14
				utils[rr] -= similarity( plan1, plan2, null, network ) ; 
				if ( Double.isNaN(utils[rr]) ) {
					log.warn( "utils is NaN; id: " + plan1.getPerson().getId() ) ;
				}
			}
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

	private static double similarity( Plan plan1, Plan plan2, StageActivityTypes stageActivities, Network network ) {
		double simil = 0. ;
		{
			List<Activity> activities1 = TripStructureUtils.getActivities(plan1, stageActivities) ;
			List<Activity> activities2 = TripStructureUtils.getActivities(plan2, stageActivities) ;
			simil += PopulationUtils.calculateSimilarity(activities1, activities2, sameActTypePenalty, sameLocationPenalty) ;
			if ( Double.isNaN(simil) ) {
				log.warn("simil is NaN; id: " + plan1.getPerson().getId() ) ;
			}
		}
		{
			List<Leg> legs1 = TripStructureUtils.getLegs(plan1 ) ;
			List<Leg> legs2 = TripStructureUtils.getLegs(plan2 ) ;
			simil += PopulationUtils.calculateSimilarity(legs1, legs2, network, sameModePenalty, sameRoutePenalty ) ;
			if ( Double.isNaN(simil) ) {
				log.warn("simil is NaN; id: " + plan1.getPerson().getId() ) ;
			}
		}		

		return simil ;
	}

}
