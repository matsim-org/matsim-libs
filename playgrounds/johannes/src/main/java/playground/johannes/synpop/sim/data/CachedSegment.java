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

package playground.johannes.synpop.sim.data;

import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class CachedSegment extends CachedElement implements Segment {

    private CachedEpisode episode;

    private boolean isLeg;

    public CachedSegment(Segment delegate) {
        super(delegate);
    }

    void setEpisode(CachedEpisode episode, boolean isLeg) {
        this.episode = episode;
        this.isLeg = isLeg;
    }

    @Override
    public Episode getEpisode() {
        return episode;
//        throw new UnsupportedOperationException("Navigation not supported.");
    }

    @Override
    public Segment next() {
        if (isLeg) {
            int index = getEpisode().getLegs().indexOf(this);
            if (index > -1) return getEpisode().getActivities().get(index + 1);
            else return null;
        } else {
            int index = getEpisode().getActivities().indexOf(this);
            if (index > -1 && index < getEpisode().getLegs().size()) {
                return getEpisode().getLegs().get(index);
            } else return null;
        }
//        throw new UnsupportedOperationException("Navigation not supported.");
    }

    @Override
    public Segment previous() {
        if (isLeg) {
            int index = getEpisode().getLegs().indexOf(this);
            if (index > -1) return getEpisode().getActivities().get(index);
            else return null;
        } else {
            int index = getEpisode().getActivities().indexOf(this);
            if (index > 0) {
                return getEpisode().getLegs().get(index - 1);
            } else return null;
        }
//        throw new UnsupportedOperationException("Navigation not supported.");
    }
}
