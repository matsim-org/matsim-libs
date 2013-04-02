/* *********************************************************************** *
 * project: org.matsim.*
 * SPConcavity.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2denvironment.approxdecomp;

import playground.gregor.sim2denvironment.approxdecomp.ApproxConvexDecomposer.PocketBridge;

import com.vividsolutions.jts.geom.Coordinate;

public class SPConcavity {

	public void computeSPConcavity(Coordinate [] shell, PocketBridge pb, double [] concavity) {
		
		

		VisibilityGraph visibilityGraph = new VisibilityGraph(shell, pb);
		
		
		
		for (int i = pb.betaMinus+1; i < pb.betaPlus; i++) {
			double val = visibilityGraph.computeSPLength(i);
			concavity[i%shell.length] = val;
		}
		
	}

	
	
	
}
