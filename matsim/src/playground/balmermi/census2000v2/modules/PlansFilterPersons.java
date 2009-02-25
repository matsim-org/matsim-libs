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
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.population.Population;

import playground.balmermi.census2000v2.data.CAtts;
import playground.balmermi.census2000v2.data.Household;

public class PlansFilterPersons {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

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
		
		// remove persons which are only part of the 'zivilrechtliche' population
		Set<Id> pids = new HashSet<Id>();
		for (Person p : plans.getPersons().values()) {
			if (p.getCustomAttributes().get(CAtts.HH_W) == null) { pids.add(p.getId()); }
		}
		for (Id pid : pids) {
			Person p = plans.getPersons().remove(pid);
			Object o = p.getCustomAttributes().get(CAtts.HH_Z);
			if (o == null) { Gbl.errorMsg("pid="+p.getId()+": no hh_z. That must not happen!"); }
			((Household)o).removePersonZ(p.getId());
		}
		log.info("      "+pids.size()+" persons without '"+CAtts.HH_W+"' household.");
		
		// remove 'zivilrechtliche' data from the population
		for (Person p : plans.getPersons().values()) {
			Object o = p.getCustomAttributes().get(CAtts.HH_Z);
			if (o != null) {
				Facility f = ((Household)o).getFacility();

				((Household)o).removePersonZ(p.getId());
				p.getCustomAttributes().remove(CAtts.HH_Z);

				if (p.getKnowledge().getActivities(CAtts.ACT_HOME).size() == 2) {
					Activity a0 = p.getKnowledge().getActivities(CAtts.ACT_HOME).get(0);
					Activity a1 = p.getKnowledge().getActivities(CAtts.ACT_HOME).get(1);
					if (a0.getFacility().getId().equals(f.getId())) {
						if (!p.getKnowledge().removeActivity(a0)) { Gbl.errorMsg("pid="+p.getId()+": That must not happen!"); }
					}
					else if (a1.getFacility().getId().equals(f.getId())) {
						if (!p.getKnowledge().removeActivity(a1)) { Gbl.errorMsg("pid="+p.getId()+": That must not happen!"); }
					}
					else {
						Gbl.errorMsg("pid="+p.getId()+": That must not happen!");
					}
				}
			}
			// checks
			if (p.getCustomAttributes().get(CAtts.HH_Z) != null) {
					Gbl.errorMsg("pid="+p.getId()+": Still containing hh_z!");
				}
			if (p.getCustomAttributes().get(CAtts.HH_W) == null) {
				Gbl.errorMsg("pid="+p.getId()+": No hh_w!");
			}
			if (p.getKnowledge().getActivities(CAtts.ACT_HOME).size() != 1) {
				Gbl.errorMsg("pid="+p.getId()+": "+ p.getKnowledge().getActivities(CAtts.ACT_HOME).size() + " home acts!");
			}
		}
		
		log.info("    done.");
	}
}
