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
package kai.expectedUtls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.distribution.GumbelDistribution;
import org.apache.commons.math3.util.FastMath;

/**
 * @author nagel
 *
 */
class KNExpectedUtilities {

	public static void main(String[] args) {
		final double EULER = - FastMath.PI / (2 * FastMath.E);

		final double fact = 1. ; // weight of eta
		final double etaInertia = 0.0; // creates correlation
		final double beta = 1. ;

		final int N_ALTERNATIVES = 100 ;

		final double N_DRAWS = 10000. ;

		GumbelDistribution gmb = new GumbelDistribution( EULER, beta ) ;

		System.out.println( "mean: " + gmb.getNumericalMean() ) ;
		System.out.println( "variance: " + gmb.getNumericalVariance() ) ;
		
		List<Double> vvv = new ArrayList<>() ;
		for ( int ii=0 ; ii<N_ALTERNATIVES ; ii++ ) {

			// generate the V_i (they are all zero):
			vvv.add( 0. ) ;

		}

		double expectedUtlSum = 0. ;
		double receivedUtlSum = 0. ;
		double receivedUtlSum2 = 0. ;
		double gammaSum = 0.0;
		double logsumSum = 0.0;
		// double diff = 0 ;
		// double sumEta = 0. ;

		// initialize eta process
		System.out.println("eta inertia = " + etaInertia);
		List<Double> eta = new ArrayList<Double>();		
		for (int ii = 0; ii < N_ALTERNATIVES; ii++) {
			eta.add(fact * gmb.sample());
		}
		
		for ( int jj=0 ; jj<N_DRAWS ; jj++ ) {
			// average over N_DRAWS:
			
			// these are the V_i + eps_i:
			List<Double> vvPlsEps = new ArrayList<>() ;			
			for ( Double vv : vvv ) {
				final double vvPlsEp = vv + gmb.sample();
				vvPlsEps.add( vvPlsEp  ) ;				
			}

			// these are the V_i + eps_i + eta_i:				
			List<Double> vvPlsEpsPlsEta = new ArrayList<>() ;
			for (int ii = 0; ii < N_ALTERNATIVES; ii++) {
				vvPlsEpsPlsEta.add( vvPlsEps.get(ii) + eta.get(ii) );				
			}			

			// compute this iteration's logsum given the current eta,
			// for "wartezimmer" averaging
			double sum = 0. ;
			for (int ii = 0; ii < N_ALTERNATIVES; ii++) {
				sum += FastMath.exp( beta * vvv.get(ii) + eta.get(ii) ) ;
			}
			logsumSum += FastMath.log( sum )/beta;			

			// this computes the index where V_i + eps_i + eta_i is max:
			final Double scoreThatAgentHopesFor = Collections.max( vvPlsEpsPlsEta );
			int index = vvPlsEpsPlsEta.indexOf( scoreThatAgentHopesFor ) ;
			expectedUtlSum += scoreThatAgentHopesFor;
			
			// decision based on average score:
			final Double averageScore = Collections.max( vvPlsEps ) ;
			int index2 = vvPlsEps.indexOf( averageScore ) ;
						
			// create new etas (to be experienced in the mobsim; these will then
			// also be the basis for the next iteration's decision)
			List<Double> newEta = new ArrayList<Double>();		
			for (int ii = 0; ii < N_ALTERNATIVES; ii++) {
				newEta.add(etaInertia * eta.get(ii) + (1.0 - etaInertia) * fact * gmb.sample());
			}
			
			// use V_i + eps_i from this and add the new eta_i (this is the utl the agent will receive) and average over that:
			final double actualScore = vvPlsEps.get(index) + newEta.get(index);			
			receivedUtlSum += actualScore ;
			
			final double actualScore2 = vvPlsEps.get(index2) + newEta.get(index2) ;
			receivedUtlSum2 += actualScore2 ;
			
			// This is the correction. It does not become zero for reasons
			// explained via Email. Using only directly available "MATSim-variables".
			final double scoreBeforeDecision = vvv.get(index) + eta.get(index);
			final double scoreThatWasReceived = vvv.get(index) + newEta.get(index);
			final double gamma = scoreThatWasReceived - scoreBeforeDecision;
			gammaSum += gamma;
			
			// take over etas
			eta = newEta;
			
			// collect some other quantities as candidates for correction:
			// sumEta += scoreThatAgentHopesFor - vvPlsEps.get(index) ;
			// diff += scoreThatAgentHopesFor - actualScore ;
		}

		System.out.println();
		System.out.println("received: " + receivedUtlSum / N_DRAWS);
		System.out.println("correction: " + gammaSum / N_DRAWS);

		System.out.println("\navailable if one knew the epsilons:");
		System.out.println("expected (from frozen epsilons): " + expectedUtlSum / N_DRAWS);
		System.out.println("expected (from frozen epsilons) - correction: "
				+ (expectedUtlSum / N_DRAWS + gammaSum / N_DRAWS) + " (same as above under _received_)");
		
		System.out.println("\navailable even if one does not know the epsilons:");
		System.out.println("expected (from logsum): " + logsumSum / N_DRAWS);
		System.out.println("expected (from logsum) - correction: "
				+ (logsumSum / N_DRAWS + gammaSum / N_DRAWS));
		
		System.out.println("\ndecision based on average score:") ;
		System.out.println("average score: " + receivedUtlSum2 / N_DRAWS ) ;
		
		// System.out.println( "numerical max is: " + receivedUtlSum/N_DRAWS ) ;
		// System.out.println( "diff is: " + diff/N_DRAWS ) ;
		// System.out.println( "av eta (before) is: " + sumEta/N_DRAWS ) ;

		// double sum = 0. ;
		// for ( Double vv : vvv ) {
		// sum += FastMath.exp( beta * vv ) ;
		// }
		//
		//		System.out.println( "logsum is: " + FastMath.log( sum )/beta ) ;


	}

}
