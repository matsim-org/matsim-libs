/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.optimizer;

import org.matsim.contrib.dvrp.data.model.Request;
import org.matsim.contrib.dvrp.data.schedule.*;


public interface VrpOptimizer
{
    void init();


    /**
     * This function can be generalized (in the future) to encompass request modification,
     * cancellation etc. See:
     * {@link org.matsim.contrib.dvrp.VrpSimEngine#requestSubmitted(Request, double)}
     */
    //return boolean? ("has anything changed?" true/false)
    void requestSubmitted(Request request);


    //return boolean? ("has anything changed?" true/false)
    void nextTask(Schedule<? extends Task> schedule);
}
