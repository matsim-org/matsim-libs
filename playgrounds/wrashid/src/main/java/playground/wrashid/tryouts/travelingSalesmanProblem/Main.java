/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.tryouts.travelingSalesmanProblem;

public class Main {

	public static void main(String[] args){
		int[][] coordinates=new int[10][2];
		
		coordinates[0][0] = 0;
		coordinates[0][1] = 0;
		
		coordinates[1][0] = 0;
		coordinates[1][1] = 0;
		
		TravelingSalesman ts=new TravelingSalesman(coordinates);
		
		ts.printCosts();
	} 
	
}
