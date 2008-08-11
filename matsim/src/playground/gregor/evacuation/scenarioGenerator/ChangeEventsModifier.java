/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeEventsModifier.java
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

package playground.gregor.evacuation.scenarioGenerator;

public class ChangeEventsModifier {
	
	
	public static void main(String [] args) {
		
		if (args.length != 4) {
			throw new RuntimeException("Wrong number of arguments!");
		}
		
		final String network = args[0];
		final String changein = args[1];
		final String changeout = args[2];
		final String barriers = args[3];
		
		
		
	}

}
