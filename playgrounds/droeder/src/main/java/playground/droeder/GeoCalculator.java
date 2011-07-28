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
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * @author droeder
 *
 */
public class GeoCalculator {
//	private static final Logger log = Logger.getLogger(GeoCalculator.class);
	
	public static double distanceBetween2Points(Coord one, Coord two){
		double a = one.getX() - two.getX();
		double b = one.getY() - two.getY();
		return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
	}
	
	public static double angleBeetween2Straights(Tuple<Coord, Coord> one, Tuple<Coord, Coord> two) {
		double value;
		Vector2D o = new Vector2D(one.getSecond().getX() - one.getFirst().getX(),  
								one.getSecond().getY() - one.getFirst().getY());
		Vector2D t = new Vector2D(two.getSecond().getX() - two.getFirst().getX(),
						two.getSecond().getY() - two.getFirst().getY());
		value = (o.scalarProduct(t)/(o.absolut() * t.absolut()));
		if(value > 1){
			if((value - 1.0) < (1.0/1000.0)){
				return Math.acos(1);
			}else{
				throw new RuntimeException("acos not defined for: " + value);
			}
		}else if(value < -1){
			if((value + 1.0) > -(1.0/1000.0) ){
				return Math.acos(-1);
			}else{
				throw new RuntimeException("acos not defined for: " + value);
			}
		}else{
			return Math.acos(value);
		}
	}
	
	public static double angleStraight2e1(Tuple<Coord, Coord> one){
		return angleBeetween2Straights(one, new Tuple<Coord, Coord>(new CoordImpl(0, 0), new CoordImpl(1,0)));
	}
	
//	public static double angleBeetween2StraightsDeg(Tuple<Coord, Coord> one, Tuple<Coord, Coord> two){
//		return (angleBeetween2Straights(one, two)/(Math.PI * 2) * 360);
//	}
	
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
