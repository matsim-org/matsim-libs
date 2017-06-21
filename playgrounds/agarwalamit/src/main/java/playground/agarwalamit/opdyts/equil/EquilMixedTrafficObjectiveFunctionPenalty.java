/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.opdyts.equil;

/**
 * Created by amit on 03.06.17.
 */


public final class EquilMixedTrafficObjectiveFunctionPenalty {

    /*
     * This is the modal share (+margin) of mode m after which there is no effect
     * of ASCs on the modal share and so as on the objective function plus. For example,
     * in equilnet example, due to 10% of mode choice, bicycle share cant be less than 10% and
     * more than 90%.
     */
    private static final double modalShareMargin = 0.15;

    /*
     * The ASC of mode m at which the modal share of the two modes are more or less same.
     */
    private static final double ascForEqualModalShare = 2.0;

    /*
     * This is something like dPenalty/dASC
     */
    private static final double scalingInFlatRegion = 1.0;

    public static double getPenalty(final double modalShare, final double asc){
        double upperPenalty = scalingInFlatRegion * Math.max(0, modalShare-(1-modalShareMargin)) * Math.abs(asc - ascForEqualModalShare);
        double lowerPenalty = scalingInFlatRegion * Math.max(0, modalShareMargin-modalShare) * Math.abs(asc - ascForEqualModalShare);
        return upperPenalty+lowerPenalty;
    }
}
