/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */

package org.matsim.contrib.drt.extension.shifts.analysis.efficiency;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.contrib.drt.extension.shifts.events.DrtShiftEndedEvent;
import org.matsim.contrib.drt.extension.shifts.events.DrtShiftEndedEventHandler;
import org.matsim.contrib.drt.extension.shifts.events.DrtShiftStartedEvent;
import org.matsim.contrib.drt.extension.shifts.events.DrtShiftStartedEventHandler;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.contrib.drt.fare.DrtFareHandler;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.core.gbl.Gbl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

/**
 * @author nkuehnel / MOIA
 */
final class ShiftEfficiencyTracker implements PersonMoneyEventHandler,
        PassengerDroppedOffEventHandler, DrtShiftStartedEventHandler, DrtShiftEndedEventHandler {

    private final Map<Id<DrtShift>, Double> revenueByShift = new HashMap<>();
    private final Map<Id<Request>, Id<DrtShift>> shiftByRequest = new HashMap<>();

    private final Map<Id<DvrpVehicle>, Id<DrtShift>> activeShifts = new HashMap<>();
    private final Map<Id<DrtShift>, Id<DvrpVehicle>> finishedShifts = new HashMap<>();

    @Override
    public void handleEvent(PersonMoneyEvent personMoneyEvent) {
        if (DrtFareHandler.PERSON_MONEY_EVENT_PURPOSE_DRT_FARE.equals(personMoneyEvent.getPurpose())) {
            Id<DrtShift> key = shiftByRequest.get(Id.create(personMoneyEvent.getReference(), DrtRequest.class));
            if(key != null) {
                revenueByShift.merge(key, -personMoneyEvent.getAmount(), Double::sum);
            }
        }
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent event) {
        Id<DvrpVehicle> vehicleId = event.getVehicleId();
        Gbl.assertIf(activeShifts.containsKey(vehicleId));
        Id<DrtShift> drtShiftId = activeShifts.get(vehicleId);
        shiftByRequest.put(event.getRequestId(), drtShiftId);
    }

    @Override
    public void handleEvent(DrtShiftStartedEvent event) {
        revenueByShift.put(event.getShiftId(), 0.);
        if(activeShifts.containsKey(event.getVehicleId())) {
            throw new RuntimeException("Vehicle is already registered for another shift");
        }
        activeShifts.put(event.getVehicleId(), event.getShiftId());
    }

    @Override
    public void handleEvent(DrtShiftEndedEvent event) {
        activeShifts.remove(event.getVehicleId());
        finishedShifts.put(event.getShiftId(), event.getVehicleId());
    }

    public Map<Id<DrtShift>, Double> getRevenueByShift() {
        return revenueByShift;
    }

    public Map<Id<DrtShift>, List<Id<Request>>> getRequestsByShift() {
        return shiftByRequest.entrySet()
                .stream()
                .collect(groupingBy(Map.Entry::getValue, mapping(Map.Entry::getKey, toList())));
    }

    public Map<Id<DrtShift>, Id<DvrpVehicle>> getFinishedShifts() {
        return finishedShifts;
    }

    @Override
    public void reset(int iteration) {
        revenueByShift.clear();
        shiftByRequest.clear();
        activeShifts.clear();
        finishedShifts.clear();
    }
}
