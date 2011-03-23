/* *********************************************************************** *
 * project: org.matsim.*
 * LPPHEV.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.wrashid.sschieffer.V1G;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class LPPHEV {
	
	private Schedule schedule;
	private LpSolve solver; 
	
	
	public LPPHEV(){
		
	}
	
	
	public Schedule solveLP(Schedule schedule) throws LpSolveException{
		this.schedule=schedule;
		
		int numberOfVariables= schedule.getNumberOfEntries()+1;
		
		solver = LpSolve.makeLp(0, numberOfVariables);
		
		
		return schedule;
		
	}
	
	

	
}
