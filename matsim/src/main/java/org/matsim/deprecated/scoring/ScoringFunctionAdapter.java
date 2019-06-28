
/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionAdapter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 /*
  ************************************************************************ *
  * project: org.matsim.*                                                  *
  * ********************************************************************** *
  *                                                                        *
  * copyright       : (C) ${year} by the members listed in the COPYING,    *
  *                   LICENSE and WARRANTY file.                           *
  * email           : info at matsim dot org                               *
  *                                                                        *
  * ********************************************************************** *
  *                                                                        *
  *   This program is free software; you can redistribute it and/or modify *
  *   it under the terms of the GNU General Public License as published by *
  *   the Free Software Foundation; either version 2 of the License, or    *
  *   (at your option) any later version.                                  *
  *   See also COPYING, LICENSE and WARRANTY file                          *
  *                                                                        *
  * ***********************************************************************

 */

package org.matsim.deprecated.scoring;


import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * An adapter from the old ScoringFunction interface to the new one.
 * Do not use for new things. Just implement ScoringFunction.
 * 
 * @author michaz
 *
 */

@Deprecated
public abstract class ScoringFunctionAdapter implements ScoringFunction {

    public final void handleActivity(Activity activity) {
        if (activity.getStartTime() != Time.UNDEFINED_TIME) {
            startActivity(activity.getStartTime(), activity);
        }
        if (activity.getEndTime() != Time.UNDEFINED_TIME) {
            endActivity(activity.getEndTime(), activity);
        }
    }

    public final void handleLeg(Leg leg) {
        startLeg(leg.getDepartureTime(), leg);
        endLeg(leg.getDepartureTime() + leg.getTravelTime());
    }

    /**
     * Tells the scoring function that the agent begins with an activity.
     *
     * @param time The time at which the mentioned activity starts.
     * @param act The activity the agent starts. Can be used to get the activity
     * type, exact location, facility, opening times and other information.
     */
    public abstract void startActivity(final double time, final Activity act);

    /**
     * Tells the scoring function that the agent stops with an activity.
     * The activity is always the same one which started previously,
     * except if it is the first activity, which never starts.
     *
     * @param time The time at which the agent stops performing the current
     * activity.
     * @param activity
     */
    public abstract void endActivity(final double time, Activity activity);

    /**
     * Tells the scoring function that the agents starts a new leg.
     *
     * @param time The time at which the agent starts the new leg.
     * @param leg The leg the agent starts. Can be used to get leg mode and other
     * information about the leg.
     */
    public abstract void startLeg(final double time, final Leg leg);

    /**
     * Tells the scoring function that the current leg ends.
     *
     * @param time The time at which the current leg ends.
     */
    public abstract void endLeg(final double time);

}
