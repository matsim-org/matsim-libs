/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.data.graph.algorithms;

import org.apache.commons.math.geometry.Vector3D;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

import playground.droeder.DistanceCalculator;

/**
 * @author droeder
 * calculates some distances between two straight lines 
 */
public class StraightsDistanceCalculator {
	
	private Tuple<Coord, Coord> straightOne, straightTwo;
	private Double dAC, dCE, dBF, dDB;
	
	public StraightsDistanceCalculator(Tuple<Coord, Coord> straightOne, Tuple<Coord, Coord> straightTwo){
		this.straightOne = straightOne;
		this.straightTwo = straightTwo;
	}
	
	public Double getAvStraightDistance(){
		return (0.5 * (Math.min(this.getdAC(), this.getdCE())+ Math.min(this.getdBF(), this.getdDB())));
	}
	
	private void calcInterception(){
		Vector3D pointA, pointB, pointC, pointD, cd, ab, temp, abOrthNorm, cdOrthNorm;
		
		pointA = this.Coord2Vector3D(straightOne.getFirst());
		pointB = this.Coord2Vector3D(straightOne.getSecond());
		pointC = this.Coord2Vector3D(straightTwo.getFirst());
		pointD = this.Coord2Vector3D(straightTwo.getSecond());
		
		ab = new Vector3D(1, pointB, -1, pointA);
		cd = new Vector3D(1, pointD, -1, pointC);
		
		// get dCE
		temp = new Vector3D(1, pointC, -1, pointA);
		temp = Vector3D.crossProduct(temp, ab);
		this.dCE = temp.getNorm()/ ab.getNorm();
		
		// get dBF
		temp = new Vector3D(1, pointB, -1, pointC);
		temp = Vector3D.crossProduct(temp, cd);
		this.dBF = temp.getNorm() / cd.getNorm();
	}
	
	private Vector3D Coord2Vector3D(Coord c){
		return new Vector3D(c.getX(), c.getY(), 0.0);
	}
	
	public Double getdAC() {
		if(this.dAC == null){
			this.dAC = DistanceCalculator.between2Points(this.straightOne.getFirst(), this.straightTwo.getFirst());
		}
		return this.dAC;
	}

	public Double getdCE() {
		if(this.dCE == null){
			this.calcInterception();
		}
		return this.dCE;
	}

	public Double getdBF() {
		if(this.dBF == null){
			this.calcInterception();
		}
		return this.dBF;
	}

	public Double getdDB() {
		if(this.dDB == null){
			this.dDB = DistanceCalculator.between2Points(straightTwo.getSecond(), straightOne.getSecond());
		}
		return dDB;
	}

//	public Double getdEB() {
//		if(this.dEB == null){
//			this.dEB = DistanceCalculator.between2Points(this.E, this.straightOne.getSecond());
//		}
//		return dEB;
//	}
//
//	public Double getdCF() {
//		if(this.dCF == null){
//			this.calcInterception();
//		}
//		return dCF;
//	}
}
