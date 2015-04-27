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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.router.*;


public interface DriveTask
    extends Task
{
    VrpPath getPath();


    /**
     * Vehicle changes its path. Can be used for: <br/>
     * - changing destination (while keepen the current task active) <br/>
     * - stopping it as soon as possible (i.e. at the end of the current/next link) <br/>
     * - random walk, roaming/crusing around <br/>
     * - ...
     */
    void pathDiverted(DivertedVrpPath divertedPath, double newEndTime);
}
