/* *********************************************************************** *
 * project: org.matsim.*
 * IdFactory.java
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

/**
 * 
 */
package playground.tnicolai.urbansim.utils;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author thomas
 *
 */

public class IdFactory {

	
	public static Id get(int i) {
		return new IdImpl(i);
	}

	public static void generateIds(int number, List<Id> idList) {
		for (int i = 1; i <= number; i++) {
			idList.add(IdFactory.get(i));
		}
	}
	
}

