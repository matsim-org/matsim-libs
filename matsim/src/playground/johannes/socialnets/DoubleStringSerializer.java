/* *********************************************************************** *
 * project: org.matsim.*
 * DoubleStringSerializer.java
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

/**
 * 
 */
package playground.johannes.socialnets;

/**
 * @author illenberger
 *
 */
public class DoubleStringSerializer implements StringSerializer<Double> {

	public String decode(Double object) {
		if(object == null)
			return "0";
		else
			return String.valueOf(object);
	}

	public Double encode(String data) {
		return new Double(data);
	}

}
