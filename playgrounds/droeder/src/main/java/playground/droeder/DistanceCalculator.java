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
package playground.droeder;

import org.apache.commons.math.geometry.Vector3D;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author droeder
 *
 */
public class DistanceCalculator {
	
	public static double between2Points(Coord one, Coord two){
		double a = one.getX() - two.getX();
		double b = one.getY() - two.getY();
		return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
	}
	
	public static double getAverageStraightDistance(Tuple<Coord, Coord> straightOne, Tuple<Coord, Coord> straightTwo){
		double dAC, dCE, dBF, dBD;
		//get dist between start/start and end/end
		dAC = between2Points(straightOne.getFirst(), straightTwo.getFirst());
		dBD = between2Points(straightOne.getSecond(), straightTwo.getSecond());
		
		// get shortest dist from the start of the first straight to the second straight
		Vector3D pointA, pointB, pointC, pointD, cd, ab, temp;
		
		pointA = Coord2Vector3D(straightOne.getFirst());
		pointB = Coord2Vector3D(straightOne.getSecond());
		pointC = Coord2Vector3D(straightTwo.getFirst());
		pointD = Coord2Vector3D(straightTwo.getSecond());
		
		ab = new Vector3D(1, pointB, -1, pointA);
		cd = new Vector3D(1, pointD, -1, pointC);
		
		// get dCE
		temp = new Vector3D(1, pointC, -1, pointA);
		temp = Vector3D.crossProduct(temp, ab);
		dCE = temp.getNorm()/ ab.getNorm();
		
		// get dBF
		temp = new Vector3D(1, pointB, -1, pointC);
		temp = Vector3D.crossProduct(temp, cd);
		dBF = temp.getNorm() / cd.getNorm();
		return (0.5 * (Math.min(dAC, dCE)+ Math.min(dBF,dBD)));
	}
	
	public static Vector3D Coord2Vector3D(Coord c){
		return new Vector3D(c.getX(), c.getY(), 0.0);
	}
}
