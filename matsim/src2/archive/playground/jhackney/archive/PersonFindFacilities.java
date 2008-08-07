/* *********************************************************************** *
 * project: org.matsim.*
 * PersonFindFacilities.java
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

package playground.jhackney.deprecated;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.facilities.Facility;
import org.matsim.gbl.Gbl;
import org.matsim.population.Knowledge;
import org.matsim.population.Person;

/**
 * This class returns an ArrayList of the facilities of a certain type that a
 * given Person knows about. Parameters: Person p, String actType (means
 * activity type)
 *
 * OBSOLETE
 *
 * @author jhackney
 */
public class PersonFindFacilities {
    public ArrayList<Facility> personGetFacilities(final Person p,
	    final String actType) {
	final Knowledge know = p.getKnowledge();
	if (know == null) {
	    Gbl.errorMsg("Knowledge is not defined!");
	}
	final TreeMap<String, ActivityFacilities> af = know.getActivityFacilities();
	ArrayList<Facility> facilities = new ArrayList<Facility>();
	final Iterator<ActivityFacilities> afIter = af.values().iterator();
	while (afIter.hasNext()) {
	    final ActivityFacilities af2 = afIter.next();
	    final String at = af2.getActType();
	    if (actType.equals(at)) {
		// only add those activities which match the desired activity
		// type
		final TreeMap<IdImpl, Integer> freqs = af2.getFrequencies();
		final Iterator<Facility> fIter = af2.getFacilities().values()
			.iterator();
		while (fIter.hasNext()) {
		    final Facility f = fIter.next();
		    final Id id = f.getId();
		    int freq = freqs.get(id);
		    if (freq < 1) {
			freq = 1;
		    }
		    for (int i = 0; i < freq; i++) {
			// Coord xy = f.getLocation().getCenter();
			facilities.add(f);
			// System.out.println(" "+i+" Coordinates of
			// "+p.getId()+" "+at+" "+coords.get(i));
		    }
		}
	    }
	}
	return facilities;
    }

}
