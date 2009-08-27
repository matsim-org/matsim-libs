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
import java.util.Collections;

import org.matsim.core.gbl.MatsimRandom;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import com.vividsolutions.jts.geom.Coordinate;

public class MyFitnessFunction {
	private ArrayList<Coordinate> points;
	private int numberOfPoints;
	private final boolean max;
	private ArrayList<Integer> precedenceVector;
	private DenseDoubleMatrix2D distanceMatrix;

	public MyFitnessFunction(boolean isMax, int number){
		this.max = isMax;
		this.numberOfPoints = number;
		this.points = new ArrayList<Coordinate>(this.numberOfPoints);	
		this.precedenceVector = generateRandomInstance(this.numberOfPoints,-100,100,-100,100);
		this.distanceMatrix = calculateDistanceMatrix();
	}	
	
	private DenseDoubleMatrix2D calculateDistanceMatrix(){
		DenseDoubleMatrix2D result = new DenseDoubleMatrix2D(points.size(), points.size());
		for(int row = 0; row < result.rows(); row++){
			for(int col = 0; col < result.columns(); col++){
				if(col > row){
					/*
					 * For simplicity, I am going to round the values.
					 */
					result.setQuick(row, col, Math.round(points.get(row).distance(points.get(col))));
				} else{
					result.setQuick(row, col, result.getQuick(col, row));
				}
			}
		}
		return result;
	}


	public Double evaluate(ArrayList<Integer> solution){
		Double fitness = 0.0;
		int fromNode = 0;
		int toNode = 0;
		for(int i = 0; i < solution.size()-1; i++){
			fromNode = solution.get(i)-1;
			toNode = solution.get(i+1)-1;
			fitness += distanceMatrix.getQuick(fromNode, toNode);
//			fitness += this.points.get(solution.get(i)-1).distance(this.points.get(solution.get(i+1)-1));
		}
		fromNode = solution.get(solution.size()-1)-1;
		toNode = solution.get(0)-1;
		fitness += distanceMatrix.getQuick(fromNode, toNode);
//		fitness += this.points.get(solution.get(solution.size()-1)-1).distance(this.points.get(solution.get(0)-1));
		
		if(fitness == null){
			throw new RuntimeException("Could not evaluate solution!");
		} 
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
		ArrayList<Integer> result = this.createPrecedenceVector();
		return result;
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
	
	public DenseDoubleMatrix2D getDistanceMatrix() {
		return distanceMatrix;
	}

		
}
