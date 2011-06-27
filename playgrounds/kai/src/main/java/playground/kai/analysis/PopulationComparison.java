/* *********************************************************************** *
 * project: kai
 * PopulationComparison.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.kai.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.util.Assert;

/**
 * @author nagel
 *
 */
public class PopulationComparison {
	// yyyyyy ConfigUtils should go to org.matsim.core.config

	// yyyy Config should go into api

	static final double xmin = 4548802. ;
	static final double xmax = 4650000. ;
	static final double ymin = 5783028. ;
	static final double ymax = 5848081. ;
	
	static int LL = 30 ;
	static int MM = 30 ;

	private static Integer xxToBin(double xx) {
		if ( xx<=xmin || xx>=xmax ) return null ;
		return (int)((xx-xmin)/(xmax-xmin)*LL) ;
	}
	
	private static Integer yyToBin(double yy) {
		if ( yy<=ymin || yy>=ymax ) return null ;
		return (int)((yy-ymin)/(ymax-ymin)*MM) ;
	}
	
	private static double binToXx(int ii) {
		double xx = xmin + ((1.*ii+.5)/LL)*(xmax-xmin) ;
		Assert.equals( xxToBin(xx) , ii ) ;
		return xx ;
	}
	
	private static double binToYy(int jj) {
		double yy = ymin + ((1.*jj+.5)/MM)*(ymax-ymin) ;
		Assert.equals( yyToBin(yy) , jj ) ;
		return yy ;
	}
	
	private static double weight( double x1, double y1, double x2, double y2 ) {
		double rr = Math.sqrt( (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) ) ;
//		return Math.pow(rr, -0.5);
		return Math.exp(-rr*rr/50000./5000.) ;
	}
	
	private void run() throws IOException {

		int simpleCnt = 0 ;
		double simpleSum = 0. ;
		int[][] cnt = new int[LL][MM] ;
		double[][] weight = new double[LL][MM];
		double[][] sum = new double[LL][MM];
//		double[][] xsum = new double[LL][MM] ;
//		double[][] ysum = new double[LL][MM] ;
		
		Scenario sc1 = null ;
		Scenario sc2 = null ;
		
		{
			Config config1 = ConfigUtils.createConfig() ;
			config1.network().setInputFile("/Users/nagel/kairuns/19jun-w-ba16ext/kairun5-incl-ba16ext.output_network.xml.gz") ;
//			config1.plans().setInputFile("/Users/nagel/kairuns/18jun-base/kairun3-incl-ba16.reduced_plans.xml.gz") ;
			config1.plans().setInputFile("/Users/nagel/kairuns/18jun-base/pop.xml.gz") ;
			sc1 = ScenarioUtils.loadScenario(config1) ;
		}

		{
			Config config2 = ConfigUtils.createConfig() ;
			config2.network().setInputFile("/Users/nagel/kairuns/19jun-w-ba16ext/kairun5-incl-ba16ext.output_network.xml.gz") ;
//			config2.plans().setInputFile("/Users/nagel/kairuns/19jun-w-ba16ext/kairun5-incl-ba16ext.reduced_plans.xml.gz") ;
			config2.plans().setInputFile("/Users/nagel/kairuns/19jun-w-ba16ext/pop.xml.gz") ;
			sc2 = ScenarioUtils.loadScenario(config2) ;
		}
		
		{
			double deltaX = binToXx(1) - binToXx(0) ;
			double deltaY = binToYy(1) - binToYy(0) ;
			System.out.println("deltaX: " + deltaX + ", deltaY: " + deltaY ) ;
		}
		
		Population pop1 = sc1.getPopulation() ;
		Population pop2 = sc2.getPopulation() ;
		
		BufferedWriter out = IOUtils.getBufferedWriter("/Users/nagel/kairuns/cmp/cmp.txt") ;
		out.append( "homeX,homeY,deltaScore,deltaLogsum,deltaBestScore\n") ;
		
		int personCnt = 0 ;
		
		for ( Person person1 : pop1.getPersons().values() ) {
			Person person2 = pop2.getPersons().get( person1.getId() ) ;
			double bestScore1 = bestScore(person1) ;
			double bestScore2 = bestScore(person2) ;
			if ( bestScore1 > 0. && bestScore2 > 0. && bestScore1-bestScore2!=0 ) { // yyyy this removes pt users!!
				double deltaBestScore = bestScore2 - bestScore1 ;
				double deltaScore = person2.getSelectedPlan().getScore() - person1.getSelectedPlan().getScore() ;
				double deltaLogsum = logsum(person2) - logsum(person1) ;
				Coord homeCoord = ((Activity)person1.getSelectedPlan().getPlanElements().get(0)).getCoord() ;
				double homeX = homeCoord.getX() + 100.* (0.5-Math.random()) ;
				double homeY = homeCoord.getY() + 100.* (0.5-Math.random()) ;
				
				String str = homeX + "," + homeY + "," + deltaScore + "," + deltaLogsum + "," + deltaBestScore + "\n" ;
				if ( personCnt < 10 ) {
					personCnt++ ;
					System.out.print( str ) ;
				}
				out.append( str ) ;

				Integer xbin = xxToBin(homeX) ;
				Integer ybin = yyToBin(homeY) ;
				if ( xbin != null && ybin != null ) {

					simpleCnt++ ;
					simpleSum += deltaLogsum ;
					
					cnt[xbin][ybin] ++ ;

//					xsum[xbin][ybin] += homeX ;
//					ysum[xbin][ybin] += homeY ;
					
					int offset = LL ;

					for ( int ii=xbin-offset ; ii<=xbin+offset; ii++ ) {
						if ( ii>=0 && ii<LL ) {
							for ( int jj=ybin-offset ; jj<=ybin+offset ; jj++ ) {
								if ( jj>=0 && jj<MM ) {
									double ww = weight( homeX, homeY, binToXx(ii), binToYy(jj) ) ;
									weight[ii][jj] += ww ;
									sum[ii][jj] += ww* deltaLogsum ;

								}
							}
						}
					}
				}
				
			}
		}
		out.close() ;
		
		int gridCnt = 0 ;
		
		BufferedWriter out2 = IOUtils.getBufferedWriter("/Users/nagel/kairuns/cmp/grid.txt") ;
		out2.append("xx,yy,val\n") ;
		for ( int ii=0 ; ii<LL ; ii++ ) {
			for ( int jj=0 ; jj<MM ; jj++ ) {
				double xx = binToXx(ii) ; double yy = binToYy(jj) ;
				if ( cnt[ii][jj] > 20. ) {
					//				if ( weight[ii][jj] > 0. ) {
					//					double xx = xsum[ii][jj]/cnt[ii][jj] ; double yy = ysum[ii][jj]/cnt[ii][jj] ;
					double val = sum[ii][jj]/weight[ii][jj] ;
					String str = xx + "," + yy + "," + val + "\n" ;
					out2.append( str ) ;
					if ( gridCnt < 10 ) {
						gridCnt++ ;
						System.out.print( str ) ;
					}
				} else {
					double val = simpleSum / simpleCnt ;
					String str = xx + "," + yy + "," + val + "\n" ;
					out2.append( str ) ;
				}
			}
		}
		out2.close() ;
	}
	
	
	private static double logsum(Person person) {
		double bestScore = bestScore(person) ;
		
		double beta = 2.0 ;
		double sum = 0. ;
		for ( Plan plan : person.getPlans() ) {
			sum += Math.exp( beta*(plan.getScore()-bestScore) ) ;
		}
		return Math.log( sum/beta + bestScore ) ;
	}
	
	private static double bestScore(Person person) {
		double bestScore = Double.NEGATIVE_INFINITY ;
		for ( Plan plan : person.getPlans() ) {
			if ( plan.getScore() > bestScore ) {
				bestScore = plan.getScore() ;
			}
		}
		return bestScore ;
	}
	
	
	public static void main(String[] args) throws IOException {
		new PopulationComparison().run() ;
	}

}
