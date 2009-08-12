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

package playground.ciarif.retailers.RetailerGA;

import java.util.ArrayList;
import java.util.Collections;

//import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import playground.ciarif.retailers.models.GravityModel;

import com.vividsolutions.jts.geom.Coordinate;

public class MyFitnessFunction {
	private ArrayList<Coordinate> points;
	private int numberOfPoints;
	private final boolean max;
	private ArrayList<Integer> precedenceVector;
	private GravityModel gm;
	//private final static Logger log = Logger.getLogger(RetailerGA.class);
	
	public MyFitnessFunction(boolean isMax, int number){
		this.max = isMax;
		this.numberOfPoints = number;
		this.points = new ArrayList<Coordinate>(this.numberOfPoints);		
		this.precedenceVector = generateRandomInstance(this.numberOfPoints,-100,100,-100,100);
	}	

	public MyFitnessFunction(boolean isMax, int number, GravityModel gm) {
		this.max = isMax;
		this.numberOfPoints = number;
		this.points = new ArrayList<Coordinate>(this.numberOfPoints);		
		this.precedenceVector = generateRandomInstance(this.numberOfPoints,-100,100,-100,100);
		this.gm = gm;
	}

	public Double evaluate(ArrayList<Integer> solution){
		Double fitness = 0.0;
		fitness = Double.valueOf(this.gm.computePotential(solution));
		return fitness;
	}

	public ArrayList<Integer> generateRandomInstance(int numberOfPoints, int xMin, int xMax, int yMin, int yMax){
		/*
		 * Create depot at point (0,0)
		 */
		Coordinate depot = new Coordinate (0, 0);
		this.points.add(depot);
		/*
		 * Create the remainder of the points
		 */
		double xDif = (double) (xMax - xMin);
		double yDif = (double) (yMax - yMin);
		for(int i = 1; i < numberOfPoints; i++){
			double x = xMin + MatsimRandom.getRandom().nextDouble()*xDif;
			double y = yMin + MatsimRandom.getRandom().nextDouble()*yDif;
			Coordinate c = new Coordinate(x, y);
			this.points.add(c);
		}
		ArrayList<Integer> precedenceVector = this.createPrecedenceVector();
		return precedenceVector;
	}	

	private ArrayList<Integer> createPrecedenceVector() {
		ArrayList<Integer> result = new ArrayList<Integer>(this.numberOfPoints);
		ArrayList<Double> distances = new ArrayList<Double>(numberOfPoints);
		Coordinate depot  = new Coordinate(0.0, 0,0);
		for (Coordinate c : this.getPoints()) {
			distances.add(depot.distance(c));
		}
		
		ArrayList<Double> newDistances = new ArrayList<Double>(numberOfPoints);
		for (Double double1 : distances) {
			newDistances.add(Double.valueOf(double1));
		}
		Collections.sort(newDistances);
		
		while(newDistances.size() > 0){
			int index = distances.indexOf(newDistances.get(0));
			result.add(index+1);
			newDistances.remove(0);
		}		
		
		return result;
	}

	public ArrayList<Coordinate> getPoints() {
		return points;
	}

	public boolean isMax() {
		return max;
	}

	public ArrayList<Integer> getPrecedenceVector() {
		return precedenceVector;
	}

	public void setPrecedenceVector(ArrayList<Integer> precedenceVector) {
		this.precedenceVector = precedenceVector;
	}

		
}
