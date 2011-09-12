/* *********************************************************************** *
 * project: org.matsim.*
 * PersonContainer.java
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
package playground.johannes.mz2005.io;

import java.util.LinkedList;
import java.util.List;

/**
 * @author illenberger
 *
 */
class PersonDataContainer {

	public String id;
	
	public int age;
	
	public int referenceDay;
	
	public List<TripDataContaienr> trips = new LinkedList<TripDataContaienr>();
	
}
