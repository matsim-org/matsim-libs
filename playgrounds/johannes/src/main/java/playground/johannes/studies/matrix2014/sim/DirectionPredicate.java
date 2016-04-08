/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.johannes.studies.matrix2014.sim;

import playground.johannes.studies.matrix2014.matrix.io.GSVMatrixWriter;
import playground.johannes.synpop.analysis.Predicate;
import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

import java.util.Random;

/**
 * @author johannes
 */
public class DirectionPredicate implements Predicate<Segment> {

    public static final String DIRECTION_KEY = GSVMatrixWriter.DIRECTION_KEY;

    public static final String OUTWARD = "outward";

    public static final String RETURN = "return";

    private final Random random;

    private final boolean applyIfMissing;

    private final String value;

    public DirectionPredicate(String value) {
        this(value, false, null);
    }

    public DirectionPredicate(String value, boolean applyIfMissing, Random random) {
        this.value = value;
        this.random = random;
        this.applyIfMissing = applyIfMissing;
    }

    @Override
    public boolean test(Segment segment) {
        String attr = segment.getAttribute(DIRECTION_KEY);

        if(attr == null && applyIfMissing) {
            attr = apply(segment);
        }

        return value.equalsIgnoreCase(attr);
    }

    private String apply(Segment segment) {
        Segment prev = segment.previous();
        Segment next = segment.next();

        String prevType = null;
        String nextType = null;

        if(prev != null) prevType = prev.getAttribute(CommonKeys.ACTIVITY_TYPE);
        if(next != null) nextType = next.getAttribute(CommonKeys.ACTIVITY_TYPE);

        String value;

        if(ActivityTypes.HOME.equalsIgnoreCase(prevType)) {
            value = OUTWARD;
        } else if(ActivityTypes.HOME.equalsIgnoreCase(nextType)) {
            value = RETURN;
        } else {
            if(random.nextDouble() < 0.5) value = OUTWARD;
            else value = RETURN;
        }

        segment.setAttribute(DIRECTION_KEY, value);

        return value;
    }
}
