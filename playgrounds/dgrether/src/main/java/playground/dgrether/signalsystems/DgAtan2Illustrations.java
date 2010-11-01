/* *********************************************************************** *
 * project: org.matsim.*
 * DgAtan2Illustrations
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;


public class DgAtan2Illustrations {
	
	
	public static void main(String[] args){
		System.out.println("atan2: " + Math.atan2(0.0, 1.0)/Math.PI * 180.0 + " degrees or " + Math.atan2(0.0, 1.0));
		System.out.println("atan2: " + Math.atan2(1.0, 0.0)/Math.PI * 180.0  + " degrees or " + Math.atan2(1.0, 0.0));
		System.out.println("atan2: " + Math.atan2(0.0, -1.0)/Math.PI * 180.0  + " degrees or " + Math.atan2(0.0, -1.0));
		
		System.out.println();
		
		System.out.println("atan2: " + Math.atan2(-0.0, 1.0)/Math.PI * 180.0 + " degrees or " + Math.atan2(-0.0, 1.0));
		System.out.println("atan2: " + Math.atan2(-1.0, 0.0)/Math.PI * 180.0 + " degrees or " + Math.atan2(-1.0, 0.0));
		System.out.println("atan2: " + Math.atan2(-0.0, -1.0)/Math.PI * 180.0 + " degrees or " + Math.atan2(-0.0, -1.0));
		
		System.out.println("atan2: " + Math.atan2(-0.0, -0.0)/Math.PI * 180.0 + " degrees or " + Math.atan2(-0.0, -0.0));

		System.out.println();
		System.out.println("atan2: " + Math.atan2(1.0, 1.0)/Math.PI * 180.0 + " degrees or " + Math.atan2(1.0, 1.0));
		System.out.println("atan2: " + Math.atan2(1.0, -1.0)/Math.PI * 180.0  + " degrees or " + Math.atan2(1.0, -1.0));

	}
}
