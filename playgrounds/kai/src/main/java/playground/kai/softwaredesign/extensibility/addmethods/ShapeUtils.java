/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.softwaredesign.extensibility.addmethods;

/**
 * @author nagel
 *
 */
class ShapeUtils {

	private ShapeUtils() {} 

	double calculateArea( Shape shape ) {
		if ( shape instanceof Circle ) {
			Circle circle = (Circle) shape ;
			return Math.PI * circle.getRadius() * circle.getRadius() ;
		} else if ( shape instanceof Square ) {
			Square square = (Square) shape ;
			return square.getSideLength() * square.getSideLength() ;
		} else {
			throw new RuntimeException("not implemented") ;
		}
	}
	
	
}
