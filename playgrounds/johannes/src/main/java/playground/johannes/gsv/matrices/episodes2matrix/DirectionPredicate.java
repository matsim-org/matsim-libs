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

        }

        return false;
    }
}
