/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.vrp.driver;

import org.matsim.api.core.v01.population.Plan;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import playground.michalm.dynamic.*;
import playground.michalm.vrp.data.MatsimVrpData;


public class VrpSchedulePlanFactory
    implements DynPlanFactory
{
    private Vehicle vehicle;
    private MatsimVrpData data;


    public VrpSchedulePlanFactory(Vehicle vehicle, MatsimVrpData data)
    {
        this.vehicle = vehicle;
        this.data = data;
    }


    @Override
    public Plan create(DynAgent agent)
    {
        return new VrpSchedulePlan(vehicle, data);
    }
}
