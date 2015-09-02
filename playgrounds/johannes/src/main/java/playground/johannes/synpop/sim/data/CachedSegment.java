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

import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.Episode;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class CachedSegment extends CachedElement implements Segment {

    public CachedSegment(Segment delegate) {
        super(delegate);
    }

    @Override
    public Episode getEpisode() {
        throw new UnsupportedOperationException("Navigation not supported.");
    }

    @Override
    public Segment next() {
        throw new UnsupportedOperationException("Navigation not supported.");
    }

    @Override
    public Segment previous() {
        throw new UnsupportedOperationException("Navigation not supported.");
    }
}
