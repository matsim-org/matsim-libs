/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.mid;

import playground.johannes.synpop.source.mid2008.processing.PersonTask;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.data.PlainEpisode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class AddReturnPlan implements PersonTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.synpop.source.mid2008.processing.PersonTask#apply(playground.johannes
	 * .gsv.synPop.PlainPerson)
	 */
	@Override
	public void apply(Person person) {
		Set<Episode> journeys = new HashSet<>();
		for (Episode p : person.getEpisodes()) {
			if ("midjourneys".equalsIgnoreCase(p.getAttribute("datasource"))) {
				journeys.add(p);
			}
		}

		for(Episode plan : journeys) {
			Episode returnPlan = ((PlainEpisode)plan).clone();
			Collections.reverse(returnPlan.getActivities());
			Collections.reverse(returnPlan.getLegs());

			person.addEpisode(returnPlan);
		}
	}

}
