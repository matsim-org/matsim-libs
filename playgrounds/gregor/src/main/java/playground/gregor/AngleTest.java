/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor;

public class AngleTest {
	
	public static void main(String [] args) {
		
		double x, y;
		x=0.1; y=1;
		double angle = getAngle(x,y);
		System.out.println(angle);
		x=1; y=0;
		angle = getAngle(x,y);
		System.out.println(angle);
		x=0; y=-1;
		angle = getAngle(x,y);
		System.out.println(angle);
		x=-1; y=0;
		angle = getAngle(x,y);
		System.out.println(angle);		
	}

	private static double getAngle(double x, double y) {
		double angle = Math.atan2(y, x)*180/Math.PI-90.;
		if (angle < 0) {
			angle += 360;
		} else if (angle == 0) {
			return angle;
		}
		return 360-angle;
	}

}
