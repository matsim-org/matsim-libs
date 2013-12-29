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

package pl.poznan.put.vrp.dynamic.data.model;

import pl.poznan.put.vrp.dynamic.data.schedule.*;


/**
 * @author michalm
 */
public interface Vehicle
{
    int getId();


    String getName();


    Depot getDepot();// TODO or just Localizable getStartLocation()?? and getEndLocation()??


    int getCapacity();


    // vehicle's time window [T0, T1) (from T0 inclusive to T1 exclusive)
    int getT0();


    int getT1();


    // max time outside the depot
    int getTimeLimit();


    Schedule<? extends Task> getSchedule();
}
