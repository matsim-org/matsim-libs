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

package playground.johannes.synpop.data;

/**
 * @author johannes
 */
public class PlainSegment extends PlainElement implements Segment {

    private Episode episode;

    private boolean isLeg;

    @Override
    public Episode getEpisode() {
        return episode;
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
    }

    void setEpisode(Episode episode, boolean isLeg) {
        this.episode = episode;
        this.isLeg = isLeg;
    }

    public PlainSegment clone() {
        PlainSegment clone = new PlainSegment();

        for (String key : keys()) {
            clone.setAttribute(key, getAttribute(key));
        }

        return clone;
    }
}
