/* *********************************************************************** *
 * project: org.matsim.*
 * PlansFilterArea.java
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

package playground.balmermi.census2000v2.modules;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.population.Person;
import org.matsim.population.Population;

import playground.balmermi.census2000v2.data.CAtts;

public class PlansFilterPersons {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final String hh_type = CAtts.HH_W;
	private final static Logger log = Logger.getLogger(PlansFilterPersons.class);

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansFilterPersons() {
		super();
		log.info("    init " + this.getClass().getName() + " module...");
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run method
	//////////////////////////////////////////////////////////////////////

	public void run(final Population plans) {
		log.info("    running " + this.getClass().getName() + " module...");
		
		Set<Id> pids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			if (p.getCustomAttributes().get(hh_type) == null) { pids.add(p.getId()); }
		}
		for (Id pid : pids) { plans.getPersons().remove(pid); }
		log.info("      "+pids.size()+" persons without '"+hh_type+"' household.");
		
		log.info("    done.");
	}
}
