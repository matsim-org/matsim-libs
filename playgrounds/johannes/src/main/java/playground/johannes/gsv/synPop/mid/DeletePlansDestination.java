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

import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Person;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;
import playground.johannes.synpop.source.mid2008.MiDValues;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class DeletePlansDestination implements PersonTask {

	@Override
	public void apply(Person person) {
		Set<Episode> remove = new HashSet<>();

		for (Episode plan : person.getEpisodes()) {
			if ("midjourneys".equalsIgnoreCase(plan.getAttribute("datasource"))) {
				for (Attributable leg : plan.getLegs()) {
					if (MiDValues.ABROAD.equals(leg.getAttribute(MiDKeys.LEG_DESTINATION))) {
						remove.add(plan);
					}
				}
			}
		}

		for (Episode plan : remove) {
			person.getEpisodes().remove(plan);
		}

	}

}
