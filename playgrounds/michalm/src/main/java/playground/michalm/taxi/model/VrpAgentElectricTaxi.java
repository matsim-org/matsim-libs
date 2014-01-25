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

package playground.michalm.taxi.model;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.extensions.electric.*;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;


public class VrpAgentElectricTaxi
    extends ElectricVehicleImpl
{
    private EnergyConsumptionModel ecm;


    public VrpAgentElectricTaxi(Id id, Link startLink, double t0, double t1, Battery battery,
            EnergyConsumptionModel ecm)
    {
        super(id, startLink, 4, t0, t1, battery);
        this.ecm = ecm;
    }
}
