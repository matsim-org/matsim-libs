/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.emission;

import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 02/12/2016.
 */
public interface EmissionCostHandler extends EventHandler {

    Map<Double, Map<Id<Vehicle>, Double>> getTimeBin2VehicleId2TotalEmissionCosts();

    Map<Double, Map<Id<Person>, Double>> getTimeBin2PersonId2TotalEmissionCosts();

    Map<Id<Vehicle>, Double> getVehicleId2TotalEmissionCosts();

    Map<Id<Person>, Double> getPersonId2TotalEmissionCosts();

    Map<String, Double> getUserGroup2TotalEmissionCosts();

    boolean isFiltering();

}