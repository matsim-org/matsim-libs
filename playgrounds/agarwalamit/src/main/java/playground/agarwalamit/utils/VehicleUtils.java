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

package playground.agarwalamit.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

/**
 * Created by amit on 13/01/2017.
 */


public final class VehicleUtils {

    private static final Logger LOGGER = Logger.getLogger(VehicleUtils.class);

    public static void addVehiclesToScenarioFromVehicleFile(final String vehiclesFile, final Scenario scenario){
        Vehicles vehs = org.matsim.vehicles.VehicleUtils.createVehiclesContainer();
        new VehicleReaderV1(vehs).readFile(vehiclesFile);

        for(VehicleType vt : vehs.getVehicleTypes().values()) {

            scenario.getVehicles().addVehicleType(vt);
        }
    }

}
