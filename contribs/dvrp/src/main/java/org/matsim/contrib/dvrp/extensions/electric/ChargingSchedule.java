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

package org.matsim.contrib.dvrp.extensions.electric;

import java.util.List;


public interface ChargingSchedule<T extends ChargeTask>
{
    Charger getCharger();


    //tasks are time-ordered
    List<T> getTasks();// unmodifiableList


    //may fail if there the task overlaps with at least one of the already scheduled ones
    void addTask(T task);


    void removeTask(T task);


    /**
     * @return null if no vehicle is being charged now
     */
    T getCurrentTask();
}
