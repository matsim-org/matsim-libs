/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import org.matsim.counts.algorithms.graphs.helper.Comp;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;
import playground.johannes.synpop.processing.EpisodeTask;

import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author johannes
 */
public class SortLegsTask implements EpisodeTask {

    private final String key;

    private final Comparator<String> comparator;

    public SortLegsTask(String key) {
        this.key = key;
        this.comparator = null;
    }

    public SortLegsTask(String key, Comparator<String> comparator) {
        this.key = key;
        this.comparator = comparator;
    }

    @Override
    public void apply(Episode episode) {
        SortedMap<String, Segment> legs = new TreeMap<>();
        if(comparator != null) legs = new TreeMap<>(comparator);

        for(Segment leg : episode.getLegs()) {
            legs.put(leg.getAttribute(key), leg);
        }

        for(Segment leg : legs.values()) {
            episode.removeLeg(leg);
        }

        for(Segment leg : legs.values()) {
            episode.addLeg(leg);
        }
    }

    public static class IntComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            int idx1 = Integer.parseInt(o1);
            int idx2 = Integer.parseInt(o2);
            return idx1 - idx2;
        }
    }
}
