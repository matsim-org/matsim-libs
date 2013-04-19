/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalBatchRun.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.dissertation;

public class MultiModalBatchRun {

	public static void main(String[] args) {
		MultiModalDemo.capacity = 500.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 750.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 1000.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 1250.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 1500.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 1750.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 2000.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 2250.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = 2500.0;
		MultiModalDemo.main(args);
		
		MultiModalDemo.capacity = Double.MAX_VALUE;
		MultiModalDemo.main(args);
	}
}
