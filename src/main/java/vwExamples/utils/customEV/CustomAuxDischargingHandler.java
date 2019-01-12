/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package vwExamples.utils.customEV;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.data.ElectricFleet;
import org.matsim.vsp.ev.data.ElectricVehicle;

import java.util.HashSet;
import java.util.Set;

/**
 * This AUX Discharge runs also when vehicles are not in use. This is handy for
 * vehicles with idle engines, such as taxis (where heating is on while the
 * vehicle is idle), but should not be used with ordinary passenger cars.
 */
public class CustomAuxDischargingHandler implements MobsimAfterSimStepListener, IterationStartsListener {
    private final ElectricFleet evFleet;
    private final int auxDischargeTimeStep;
    private final Fleet fleet;
    private final ChargingInfrastructure chargers;
    private final Set<Id<Link>> chargerLinkIdSet = new HashSet<>();

    @Inject
    public CustomAuxDischargingHandler(ElectricFleet evFleet, Fleet fleet, ChargingInfrastructure chargers,
                                       EvConfigGroup evConfig) {
        this.evFleet = evFleet;
        this.fleet = fleet;
        this.auxDischargeTimeStep = evConfig.getAuxDischargeTimeStep();
        this.chargers = chargers;
    }

    @Override
    public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
        if ((e.getSimulationTime() + 1) % auxDischargeTimeStep == 0) {

            for (ElectricVehicle ev : evFleet.getElectricVehicles().values()) {

                // Check if vehicle is actually at any depot/charger, in this case, aux energy
                // consumption is zero
                Vehicle veh = fleet.getVehicles().get(ev.getId());

                Schedule currentSchedule = veh.getSchedule();

                Id<Link> currentLinkId = null;

                if ((currentSchedule.getStatus() == ScheduleStatus.STARTED)) {
                    System.out.println(currentSchedule.getStatus());
//					currentLinkId = ((DrtStayTask) veh.getSchedule().getCurrentTask()).getLink().getId();
//					System.out.println(currentLinkId);

                }

                // Id<Link> currentLinkId = ((DrtStayTask)
                // veh.getSchedule().getCurrentTask()).getLink().getId();

                // if (chargerLinkIdSet.contains(currentLinkId)) {
                // double energy = 0.0;
                // ev.getBattery().discharge(energy);
                //
                // } else {
                // double energy =
                // ev.getAuxEnergyConsumption().calcEnergyConsumption(auxDischargeTimeStep);
                // ev.getBattery().discharge(energy);
                // }

            }
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        for (Charger charger : chargers.getChargers().values()) {
            chargerLinkIdSet.add(charger.getLink().getId());
        }

    }

}
