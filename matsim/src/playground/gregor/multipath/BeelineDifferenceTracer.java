/* *********************************************************************** *
 * project: org.matsim.*
 * BeelineDifferenceTracer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.multipath;

import org.matsim.utils.geometry.shared.Coord;

/**
 * The BeelineDifferenceTracer calculates the deviance from the hypothetical OD connection for a given Coord.
 * This information could be used as a trace of the exploration. If the router algorithm tries to explore a node
 * that has already been explored, the algorithm can decide weather it is worth to vistit this node again based on 
 * the difference of the trace.
 * 
 * 
 * @author laemmel
 *
 */
public class BeelineDifferenceTracer {
	
	private Coord orig;
	private Coord dest;
	
	private double distOD;
	private double sqrDistOD;
	
	private final double TRACE_WEIGHT;
	
	private double crit = 1;
	private final double coef = 0.99;
	
	public BeelineDifferenceTracer(Coord orig, Coord dest){
		
		TRACE_WEIGHT = 1;
		
		this.orig = orig;
		this.dest = dest;
		
		this.distOD = this.orig.calcDistance(this.dest);
		this.sqrDistOD = Math.pow(this.distOD,2);
		
	}
	
	public void increaseCrit(){
		this.crit = Math.min(1.1, this.crit * (1/coef));
		System.out.println("new sim crit: " + this.crit);
	}
	public void decreaseCrit(){
		this.crit *= coef;
		System.out.println("new sim crit: " + this.crit);
	}

	//	TODO find a better criterion ...  
	public boolean tracesDiffer(double a, double b){
//		if (Math.min(a, b) < 0.02)
//			return false;
	
		if (Math.abs(a-b) < this.sqrDistOD * 0.01)
			return false;
		return Math.min(a, b) /Math.max(a, b) < this.crit;
	}
	
	//TODO take old deviance into account to make the trace function smoother ...
	public double getTrace(double oldTrace, Coord  oldCoord, double linkLength, Coord newCoord){
		double newDiff = getDeviance(newCoord);
		newDiff = newDiff >= 0 ? newDiff : 0;
//		
		double oldDiff = getDeviance(oldCoord);
		oldDiff = oldDiff >= 0 ? oldDiff : 0;
//		
		double directionSign = this.orig.calcDistance(newCoord) > this.orig.calcDistance(oldCoord) ? 1 : -1;
		
		
		
		double base = oldDiff * linkLength;
		double tmp = newDiff - oldDiff;
		
//		double extSign = tmp > 0 ? 1 : -1;
		double ext = Math.sqrt(Math.abs(Math.pow(linkLength, 2) - Math.pow(tmp, 2))) * tmp;
		
		double deviance = directionSign * (base + ext);
//		double deviance = (linkLength * (newDiff+oldDiff)/2);
		
		
		double newTrace = deviance + (TRACE_WEIGHT) * oldTrace; 
//			(1-TRACE_WEIGHT) * oldTrace + TRACE_WEIGHT * deviance + oldTrace;
		
		return newTrace;
	}
	
	
	private double getDeviance(Coord explored){
		
		double distExplDest = explored.calcDistance(this.dest);
		double distOrigExpl = this.orig.calcDistance(explored);

		double cosAlpha = getCosine(distExplDest,distOrigExpl);
		
		double cosBeta = Math.cos(Math.asin(cosAlpha));
		
		return cosBeta * distOrigExpl; 
		
		
	}
	
	
	/**
	 * This methode is a implementation of cosine's law and returns the cosine alpha
	 *  
	 * @param a length of side a
	 * @param c length of side c
	 * @return cosine alpha
	 */
	private double getCosine(double a, double c){
		
		return ( this.sqrDistOD  + Math.pow(c, 2) - Math.pow(a,2) ) /( 2 *this.distOD * c); 
		
	}

		
	
	//debug
	public static void main(String [] args){
		Coord A = new Coord(0,0);
		Coord B1 = new Coord(10,0);
		Coord B2 = new Coord(0,10);
		Coord C = new Coord(20,20);
		BeelineDifferenceTracer dt = new BeelineDifferenceTracer(A,C);
		double dist1 = dt.getDeviance(B1);
		double dist2 = dt.getDeviance(B2);
		System.out.println(dist1 + " " +dist2);
		
		
	}
	

}
