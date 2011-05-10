/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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
package playground.droeder.Analysis.Trips.V3;

import org.matsim.api.core.v01.population.Person;
import org.matsim.population.filters.AbstractPersonFilter;

/**
 * @author droeder
 *
 */
public class Plan2TripsFilterV3 extends AbstractPersonFilter{
	
	@Override
	public void run(Person p){
		if(judge(p)){
			super.count();
		}
	}

	@Override
	public boolean judge(Person person) {
		// TODO Auto-generated method stub
		return false;
	}

}
