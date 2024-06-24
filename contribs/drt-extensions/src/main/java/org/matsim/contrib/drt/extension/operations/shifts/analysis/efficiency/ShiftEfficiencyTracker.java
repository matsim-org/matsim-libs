/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */

package org.matsim.contrib.drt.extension.operations.shifts.analysis.efficiency;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftEndedEvent;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftEndedEventHandler;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftStartedEvent;
import org.matsim.contrib.drt.extension.operations.shifts.events.DrtShiftStartedEventHandler;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
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
public final class ShiftEfficiencyTracker implements PersonMoneyEventHandler,
        PassengerDroppedOffEventHandler, DrtShiftStartedEventHandler, DrtShiftEndedEventHandler {

    private final String mode;
    private Map<Id<DrtShift>, Double> revenueByShift;
    private Map<Id<Request>, Id<DrtShift>> shiftByRequest;

    private Map<Id<DrtShift>, Id<DvrpVehicle>> finishedShifts;
    private final Map<Id<DvrpVehicle>, Id<DrtShift>> activeShifts = new HashMap<>();

	private Record currentRecord;

	public record Record(Map<Id<DrtShift>, Double> revenueByShift,
								Map<Id<Request>, Id<DrtShift>> shiftByRequest,
								Map<Id<DrtShift>, Id<DvrpVehicle>> finishedShifts){
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
	}

	public ShiftEfficiencyTracker(String mode) {
        this.mode = mode;
        this.revenueByShift = new HashMap<>();
		this.shiftByRequest = new HashMap<>();
		this.finishedShifts = new HashMap<>();
		this.currentRecord = new Record(revenueByShift, shiftByRequest, finishedShifts);
	}

    @Override
    public void handleEvent(PersonMoneyEvent personMoneyEvent) {
        if(personMoneyEvent.getTransactionPartner().equals(mode)) {
            if (DrtFareHandler.PERSON_MONEY_EVENT_PURPOSE_DRT_FARE.equals(personMoneyEvent.getPurpose())) {
                Id<DrtShift> key = shiftByRequest.get(Id.create(personMoneyEvent.getReference(), DrtRequest.class));
                if (key != null) {
                    revenueByShift.merge(key, -personMoneyEvent.getAmount(), Double::sum);
                }
            }
        }
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent event) {
        if(event.getMode().equals(mode)) {
            Id<DvrpVehicle> vehicleId = event.getVehicleId();
            Gbl.assertIf(activeShifts.containsKey(vehicleId));
            Id<DrtShift> drtShiftId = activeShifts.get(vehicleId);
            shiftByRequest.put(event.getRequestId(), drtShiftId);
        }
    }

    @Override
    public void handleEvent(DrtShiftStartedEvent event) {
        if(event.getMode().equals(mode)) {
            revenueByShift.put(event.getShiftId(), 0.);
            if (activeShifts.containsKey(event.getVehicleId())) {
                throw new RuntimeException("Vehicle is already registered for another shift");
            }
            activeShifts.put(event.getVehicleId(), event.getShiftId());
        }
    }

    @Override
    public void handleEvent(DrtShiftEndedEvent event) {
        if(event.getMode().equals(mode)) {
            activeShifts.remove(event.getVehicleId());
            finishedShifts.put(event.getShiftId(), event.getVehicleId());
        }
    }

    @Override
    public void reset(int iteration) {
        this.revenueByShift = new HashMap<>();
        this.shiftByRequest = new HashMap<>();
        this.finishedShifts = new HashMap<>();
		this.currentRecord = new Record(revenueByShift, shiftByRequest, finishedShifts);
        this.activeShifts.clear();
    }

	public Record getCurrentRecord() {
		return currentRecord;
	}
}
