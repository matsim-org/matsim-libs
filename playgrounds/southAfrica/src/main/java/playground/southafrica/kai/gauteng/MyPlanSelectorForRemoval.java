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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.selectors.AbstractPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.PopulationUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.apache.log4j.Logger ;

/**
 * @author nagel
 *
 */
final class MyPlanSelectorForRemoval extends AbstractPlanSelector {
	static private final Logger log = Logger.getLogger(MyPlanSelectorForRemoval.class);
	
	private static final double sameActTypeReward = 1;
	private static final double sameLocationReward = 1;
	private static final double sameRouteReward = 1;
	private static final double sameModeReward = 1;
	private final Network network;
	
	public MyPlanSelectorForRemoval( Network network ) {
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
			pp++ ;
		}

		int rr=0 ;
		for ( Plan plan1 : plans ) {
			for ( Plan plan2 : plans ) {
				utils[rr] -= similarity( plan1, plan2, null, network ) ; 
				if ( Double.isNaN(utils[rr]) ) {
					log.warn( "utils is NaN" ) ;
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

	static double similarity( Plan plan1, Plan plan2, StageActivityTypes stageActivities, Network network ) {
		double simil = 0. ;
		{
			List<Activity> activities1 = TripStructureUtils.getActivities(plan1, stageActivities) ;
			List<Activity> activities2 = TripStructureUtils.getActivities(plan2, stageActivities) ;
			simil += PopulationUtils.calculateSimilarity(activities1, activities2, sameActTypeReward, sameLocationReward) ;
		}
		{
			List<Leg> legs1 = TripStructureUtils.getLegs(plan1 ) ;
			List<Leg> legs2 = TripStructureUtils.getLegs(plan2 ) ;
			simil += PopulationUtils.calculateSimilarity(legs1, legs2, network, sameModeReward, sameRouteReward ) ;
		}		

		return simil ;
	}

}
