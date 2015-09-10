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

package playground.johannes.gsv.matrices.episodes2matrix;

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Segment;

/**
 * @author johannes
 */
public class DirectionPredicate implements LegPredicate {

    public static final String OUTWARD = "out";

    public static final String RETURN = "return";

    public static final String INTERMEDIATE = "inter";

    private final String mode;

    public DirectionPredicate(String mode) {
        this.mode = mode;
    }

    @Override
    public boolean test(Segment leg) {
        if(mode.equals(OUTWARD)) {
            String prev = leg.previous().getAttribute(CommonKeys.ACTIVITY_TYPE);
            if(ActivityTypes.HOME.equalsIgnoreCase(prev)) return true;
            else return false;
        } else if(mode.equals(RETURN)) {
            String next = leg.next().getAttribute(CommonKeys.ACTIVITY_TYPE);
            if(ActivityTypes.HOME.equalsIgnoreCase(next)) return true;
            else return false;
        } else if(mode.equals(INTERMEDIATE)) {
            String prev = leg.previous().getAttribute(CommonKeys.ACTIVITY_TYPE);
            String next = leg.next().getAttribute(CommonKeys.ACTIVITY_TYPE);

            if(!prev.equalsIgnoreCase(ActivityTypes.HOME) && !next.equalsIgnoreCase(ActivityTypes.HOME)) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }
}
