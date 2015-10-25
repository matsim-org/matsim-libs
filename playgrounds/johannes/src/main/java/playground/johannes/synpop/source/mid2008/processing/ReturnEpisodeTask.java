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

package playground.johannes.synpop.source.mid2008.processing;

import org.apache.log4j.Logger;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.processing.PersonTask;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class ReturnEpisodeTask implements PersonTask {

	private static final Logger logger = Logger.getLogger(ReturnEpisodeTask.class);

	@Override
	public void apply(Person person) {
		int count = 0;

		Set<Episode> journeys = new HashSet<>();
		for (Episode p : person.getEpisodes()) {
			if(p.getLegs().size() == 1)
				journeys.add(p);
			 else {
				count++;
			}
		}

		if(count > 0) {
			logger.warn(String.format("There are %s episodes with more than one leg. Are you sure this is a journeys " +
					"file?", count));
		}

		PlainFactory factory = new PlainFactory();
		for(Episode episode : journeys) {
			Episode returnEpisode = PersonUtils.shallowCopy(episode, factory);

			for(int i = episode.getActivities().size() - 1; i >= 0; i--) {
				Segment clone = PersonUtils.shallowCopy(episode.getActivities().get(i), factory);
				returnEpisode.addActivity(clone);
			}

			for(int i = episode.getLegs().size() - 1; i >= 0; i--) {
				Segment clone = PersonUtils.shallowCopy(episode.getLegs().get(i), factory);
				clone.removeAttribute(MiDKeys.LEG_ORIGIN);
				returnEpisode.addLeg(clone);
			}

			person.addEpisode(returnEpisode);
		}
	}
}
