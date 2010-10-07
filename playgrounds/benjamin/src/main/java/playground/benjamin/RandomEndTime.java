/* *********************************************************************** *
 * project: org.matsim.*
 * RandomEndTime.java
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
package playground.benjamin;

import java.util.Random;

/**
 * @author benjamin
 *
 */
public class RandomEndTime {

	public static void main(String[] args) {
		Random random = new Random();
	
		for (int i=0; i<10000; i++){
			double r1 = random.nextDouble();
			double r2 = random.nextDouble();
			
			double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
			double endTime = 60*60 * normal + 8*60*60;
		System.out.println(i+1 +"\t" + endTime);
		}
	}
}