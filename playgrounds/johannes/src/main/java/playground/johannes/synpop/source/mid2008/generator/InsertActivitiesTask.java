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

package playground.johannes.synpop.source.mid2008.generator;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Factory;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;

/**
 * @author johannes
 */
public class InsertActivitiesTask implements EpisodeTask {

    private final Factory factory;

    public InsertActivitiesTask(Factory factory) {
        this.factory = factory;
    }

    @Override
    public void apply(Episode episode) {
        int nLegs = episode.getLegs().size();

        for (int i = 0; i < nLegs + 1; i++) {
            Segment activity = factory.newSegment();
            episode.addActivity(activity);
        }

    }

}
