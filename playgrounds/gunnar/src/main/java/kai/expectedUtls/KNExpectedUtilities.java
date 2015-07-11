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
		final double fact = 1. ; // weight of eta

		final double EULER = - FastMath.PI / (2 * FastMath.E);

		final double beta = 1. ;
		GumbelDistribution gmb = new GumbelDistribution( EULER, beta ) ;

		System.out.println( "mean: " + gmb.getNumericalMean() ) ;
		System.out.println( " variance: " + gmb.getNumericalVariance() ) ;

		final int NNN = 100 ;

		List<Double> vvv = new ArrayList<>() ;
		for ( int ii=0 ; ii<NNN ; ii++ ) {

			// generate the V_i (they are all zero):
			vvv.add( 0. ) ;

		}

		final double N_DRAWS = 10000. ;
		double utlsSum = 0. ;
		for ( int jj=0 ; jj<N_DRAWS ; jj++ ) {
			// average over N_DRAWS:
			
			List<Double> vvPlsEps = new ArrayList<>() ;
			List<Double> vvPlsEpsPlsEta = new ArrayList<>() ;
			for ( Double vv : vvv ) {
				// these are the V_i + eps_i:
				final double vvPlsEp = vv + gmb.sample();
				vvPlsEps.add( vvPlsEp  ) ;
				
				// these are the V_i + eps_i + eta_i:
				vvPlsEpsPlsEta.add( vvPlsEp + fact * gmb.sample() );
			}
			
			// this computes the index where V_i + eps_i + eta_i is max:
			int index = vvPlsEpsPlsEta.indexOf( Collections.max( vvPlsEpsPlsEta ) ) ;
			
			// use V_i + eps_i from this and add another eta_i (this is the utl the agent will receive) and average over that:
			utlsSum += vvPlsEps.get(index)  + gmb.sample() ;
			
		}

		System.out.println( "numerical max is: " + utlsSum/N_DRAWS ) ;
		
		double sum = 0. ;
		for ( Double vv : vvv ) {
			sum += FastMath.exp( beta * vv ) ;
		}

		System.out.println( "logsum is: " + FastMath.log( sum )/beta ) ;


	}

}
