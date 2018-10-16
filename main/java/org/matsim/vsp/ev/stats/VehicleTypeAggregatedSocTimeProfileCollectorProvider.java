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

package org.matsim.vsp.ev.stats;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector;
import org.matsim.contrib.util.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.util.timeprofile.TimeProfiles;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.vsp.ev.EvUnitConversions;
import org.matsim.vsp.ev.data.ElectricFleet;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class VehicleTypeAggregatedSocTimeProfileCollectorProvider implements Provider<MobsimListener> {
    private final ElectricFleet evFleet;
    private final MatsimServices matsimServices;

    @Inject
    public VehicleTypeAggregatedSocTimeProfileCollectorProvider(ElectricFleet evFleet, MatsimServices matsimServices) {
        this.evFleet = evFleet;
        this.matsimServices = matsimServices;
    }

    @Override
    public MobsimListener get() {
        ProfileCalculator calc = createIndividualSocCalculator(evFleet);
        return new TimeProfileCollector(calc, 300, "average_soc_time_profiles", matsimServices);
    }

    public static ProfileCalculator createIndividualSocCalculator(final ElectricFleet evFleet) {

        Set<String> vehicleTypes = evFleet.getElectricVehicles().values().stream().map(electricVehicle -> electricVehicle.getVehicleType()).collect(Collectors.toCollection(LinkedHashSet::new));
        vehicleTypes.add("Fleet Average");
        String[] header = vehicleTypes.stream().toArray(String[]::new);
        return TimeProfiles.createProfileCalculator(header, () -> {
                    Double[] result = new Double[header.length];
                    for (int i = 0; i < header.length - 1; i++) {
                        String type = header[i];
                        result[i] = evFleet.getElectricVehicles().values().stream().filter(electricVehicle -> electricVehicle.getVehicleType().equals(type)).mapToDouble(ev -> ev.getBattery().getSoc() / EvUnitConversions.J_PER_kWh).average().orElse(Double.NaN);
                    }
                    result[header.length - 1] = evFleet.getElectricVehicles().values().stream()//
                            .mapToDouble(ev -> ev.getBattery().getSoc() / EvUnitConversions.J_PER_kWh).average().getAsDouble();
                    return result;
                }
        );
    }

}
