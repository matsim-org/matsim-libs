/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningIdGenerator.java
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

package playground.christoph.withinday.replanning;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

public class ReplanningIdGenerator {
	
	private static int idCount = 0;
	
	public static Id getNextId() {
		Id id = new IdImpl(idCount);
		idCount++;
		
		return id;
	}
}
