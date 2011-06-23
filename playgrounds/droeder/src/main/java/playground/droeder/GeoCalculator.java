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

import java.util.Vector;

import org.apache.commons.math.geometry.Vector3D;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;

import playground.droeder.data.graph.MatchingEdge;

/**
 * @author droeder
 *
 */
public class GeoCalculator {
	
	public static double distanceBetween2Points(Coord one, Coord two){
		double a = one.getX() - two.getX();
		double b = one.getY() - two.getY();
		return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
	}
	
	public static double angleBeetween2Straights(Tuple<Coord, Coord> one, Tuple<Coord, Coord> two) {
		double absOne, absTwo, scalar;
		Vector<Double> o = new Vector<Double>();
		Vector<Double> t = new Vector<Double>();
		
		o.add(0, one.getSecond().getX() - one.getFirst().getX());
		o.add(1, one.getSecond().getY() - one.getFirst().getY());
		
		t.add(0, two.getSecond().getX() - two.getFirst().getX());
		t.add(1, two.getSecond().getY() - two.getFirst().getY());
		
		absOne = Math.sqrt(Math.pow(o.get(0), 2) + Math.pow(o.get(1), 2));
		absTwo = Math.sqrt(Math.pow(t.get(0), 2) + Math.pow(t.get(1), 2));
		
		scalar = ((o.get(0)*t.get(0)) + (o.get(1)+ t.get(1)));
		
		return Math.acos(scalar/(absOne * absTwo));
	}
	
	@Deprecated
	public static double averageStraightDistance(Tuple<Coord, Coord> straightOne, Tuple<Coord, Coord> straightTwo){
		double dAC, dCE, dBF, dBD;
		//get dist between start/start and end/end
		dAC = distanceBetween2Points(straightOne.getFirst(), straightTwo.getFirst());
		dBD = distanceBetween2Points(straightOne.getSecond(), straightTwo.getSecond());
		
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
	
	private static Vector3D Coord2Vector3D(Coord c){
		return new Vector3D(c.getX(), c.getY(), 0.0);
	}
}
