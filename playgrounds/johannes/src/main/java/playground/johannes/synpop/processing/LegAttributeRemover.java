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

package playground.johannes.synpop.processing;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

import java.util.HashSet;
import java.util.Set;

/**
 * @author johannes
 */
public class LegAttributeRemover implements EpisodeTask {

    private Set<String> attributes;

    public LegAttributeRemover(String ... attributes) {
        this.attributes = new HashSet<>(attributes.length);
        for(String att : attributes) this.attributes.add(att);
    }

    public LegAttributeRemover() {
        this.attributes = new HashSet<>();
    }

    public void addAttribute(String att) {
        attributes.add(att);
    }

    @Override
    public void apply(Episode episode) {
        for(Segment s : episode.getLegs()) {
            for(String att : attributes)
                s.removeAttribute(att);
        }
    }
}
