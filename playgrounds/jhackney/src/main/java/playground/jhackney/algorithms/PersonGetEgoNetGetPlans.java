/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.jhackney.algorithms;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import playground.jhackney.socialnetworks.socialnet.EgoNet;

public class PersonGetEgoNetGetPlans {

public PersonGetEgoNetGetPlans(){
	super();
}

	public Population extract(final Person ego) throws Exception{

		Population socialPlans= new ScenarioImpl().getPopulation();

		socialPlans.addPerson(ego);
		ArrayList<Person> alters = ((EgoNet)ego.getCustomAttributes().get(EgoNet.NAME)).getAlters();
		Iterator<Person> a_it=alters.iterator();
		while(a_it.hasNext()){
			socialPlans.addPerson(a_it.next());
		}
		return socialPlans;
	}
}
