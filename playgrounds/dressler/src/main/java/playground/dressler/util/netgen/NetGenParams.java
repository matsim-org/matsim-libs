/* *********************************************************************** *
 * project: org.matsim.*
 * NetGenParams.java
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

/**
 * @author Daniel Dressler
 *
 */

package playground.dressler.util.netgen;

public class NetGenParams {
	long randseed = 0;	
	
	// grid size
	int xdim = 50;
	int ydim = 50;		
	
	// sources, sinks
	int numbersinks = 10;
	int numbersources = 0; // 0 = potentially every vertex
	int totalsupply = 10000;
	
	// arcs, with normal distributions
	float lengthmean = 10;		
	float lengthvariance = 5;
	float capmean = 10;
	float capvariance = 5;
	
	public String toString() {
		String s;
		s = "% X " + xdim + " Y " + ydim + "\n";
		s += "% lengths mean " + lengthmean + " var " + lengthvariance + "\n";
		s += "% caps mean " + capmean + " var " + capvariance + "\n";
		s += "% sources " + numbersources + "\n";
		s += "% totalsupply " + totalsupply + "\n";
		s += "% sinks " + numbersinks + "\n";
		s += "% seed " + randseed + "\n";
		return s;	
	}
	
	public String defaultName() {
		String s;
		s = "grid";
		s += "_" + xdim + "x" + ydim;
		s += "_" + totalsupply;
		if (numbersources > 0) {
			s += "_at_" + numbersources;
		} else {
			s += "_at_all";
		}
		s += "_to_" + numbersinks;
		s += "_R" + randseed; 
		return s;
	}
}
