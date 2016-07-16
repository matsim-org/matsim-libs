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

package playground.michalm.taxi.optimizer.assignment;

import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData;

import playground.michalm.ev.data.Charger;
import playground.michalm.taxi.ev.ETaxiChargingLogic;
import playground.michalm.taxi.optimizer.assignment.AssignmentChargerPlugData.ChargerPlug;


class AssignmentChargerPlugData
    extends AssignmentDestinationData<ChargerPlug>
{
    class ChargerPlug
    {
        public final Charger charger;
        public final int idx;


        private ChargerPlug(Charger charger, int idx)
        {
            this.charger = charger;
            this.idx = idx;
        }
    }


    AssignmentChargerPlugData(TaxiOptimizerContext optimContext, Iterable<Charger> chargers)
    {
        double currTime = optimContext.timer.getTimeOfDay();

        int idx = 0;
        for (Charger c : chargers) {
            ETaxiChargingLogic logic = (ETaxiChargingLogic)c.getLogic();

            int dispatched = logic.getAssignedCount();
            int queued = logic.getQueuedCount();
            int plugged = logic.getPluggedCount();

            int assignedVehicles = plugged + queued + dispatched;
            if (assignedVehicles == 2 * c.getPlugs()) {
                continue;
            }
            else if (assignedVehicles > 2 * c.getPlugs()) {
                throw new IllegalStateException();//XXX temp check
            }

            int unassignedPlugs = Math.max(c.getPlugs() - assignedVehicles, 0);
            for (int p = 0; p < unassignedPlugs; p++) {
                ChargerPlug plug = new ChargerPlug(c, p);
                entries.add(new DestEntry<ChargerPlug>(idx++, plug, c.getLink(), currTime));
            }

            //we do not want to have long queues at chargers: 1 awaiting veh per plug is the limit
            //moreover, in a single run we can assign up to one veh to each plug
            //(sequencing is not possible with AP)
            int assignableVehicles = Math.min(2 * c.getPlugs() - assignedVehicles, c.getPlugs());
            if (assignableVehicles == unassignedPlugs) {
                continue;
            }

            double chargeStart = currTime
                    + logic.estimateAssignedWorkload() / (c.getPlugs() - unassignedPlugs);
            for (int p = unassignedPlugs; p < assignableVehicles; p++) {
                ChargerPlug plug = new ChargerPlug(c, p);
                entries.add(new DestEntry<ChargerPlug>(idx++, plug, c.getLink(), chargeStart));
            }
        }
    }
}
