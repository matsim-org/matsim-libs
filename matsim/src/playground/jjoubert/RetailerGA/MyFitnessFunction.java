/* *********************************************************************** *
 * project: org.matsim.*
 * MyTsp.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.RetailerGA;

import java.util.ArrayList;

import org.matsim.core.gbl.MatsimRandom;

import com.vividsolutions.jts.geom.Coordinate;

public class MyFitnessFunction {
	private ArrayList<Coordinate> points;
	private int numberOfPoints;

	public MyFitnessFunction(int number){
		this.numberOfPoints = number;
		this.points = new ArrayList<Coordinate>(this.numberOfPoints);		
		generateRandomInstance(this.numberOfPoints,0,100,0,100);
	}	

	public Double evaluate(ArrayList<Integer> solution){
		Double fitness = 0.0;
		
		for(int i = 0; i < solution.size()-1; i++){
			fitness += this.points.get(solution.get(i)-1).distance(this.points.get(solution.get(i+1)-1));
		}
		fitness += this.points.get(solution.get(solution.size()-1)-1).distance(this.points.get(solution.get(0)-1));
		
		if(fitness == null){
			System.err.println("Could not evaluate solution!");
			System.exit(0);
		} 
		return fitness;
	}

	public void generateRandomInstance(int numberOfPoints, int xMin, int xMax, int yMin, int yMax){
		double xDif = (double) (xMax - xMin);
		double yDif = (double) (yMax - yMin);
		for(int i = 0; i < numberOfPoints; i++){
			double x = xMin + MatsimRandom.getRandom().nextDouble()*xDif;
			double y = yMin + MatsimRandom.getRandom().nextDouble()*yDif;
			Coordinate c = new Coordinate(x, y);
			this.points.add(c);
		}
	}	

	public ArrayList<Coordinate> getPoints() {
		return points;
	}
		
}
