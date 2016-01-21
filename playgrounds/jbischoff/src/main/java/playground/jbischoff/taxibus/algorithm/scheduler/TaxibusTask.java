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

package playground.jbischoff.taxibus.algorithm.scheduler;

import org.matsim.contrib.dvrp.schedule.*;


public interface TaxibusTask
    extends Task
{
    static enum TaxibusTaskType
    {
    	        DRIVE_EMPTY, // drive empty might be needed later.
        STAY, //not directly related to any customer (although may be related to serving a customer; e.g. a pickup drive)
        PICKUP, DRIVE_WITH_PASSENGER, DROPOFF;//serving n customers (TaxibusTaskWithRequests)
    }


    TaxibusTaskType getTaxibusTaskType();
}
