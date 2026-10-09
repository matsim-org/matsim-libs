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
    // start time and shift type captured from the started event, so the analysis can be driven purely from the shift
    // lifecycle events (actual run time = ended − started, type from the event) without resolving the shift
    // specification. This is both more honest (actual vs. planned duration) and works for transient shifts that have no
    // persistent specification (e.g. remote-guidance virtual shifts).
    private Map<Id<DrtShift>, Double> actualStartByShift;
    private Map<Id<DrtShift>, Double> actualEndByShift;
    private Map<Id<DrtShift>, String> shiftTypeById;

	private Record currentRecord;

	public record Record(Map<Id<DrtShift>, Double> revenueByShift,
								Map<Id<Request>, Id<DrtShift>> shiftByRequest,
								Map<Id<DrtShift>, Id<DvrpVehicle>> finishedShifts,
								Map<Id<DrtShift>, Double> actualStartByShift,
								Map<Id<DrtShift>, Double> actualEndByShift,
								Map<Id<DrtShift>, String> shiftTypeById){
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

		/** Actual shift start time in [s] (from the started event), per finished shift. */
		public Map<Id<DrtShift>, Double> getActualStartByShift() {
			return actualStartByShift;
		}

		/** Actual shift end time in [s] (from the ended event), per finished shift. */
		public Map<Id<DrtShift>, Double> getActualEndByShift() {
			return actualEndByShift;
		}

		/** Shift type per finished shift, taken from the shift lifecycle events (no specification lookup). */
		public Map<Id<DrtShift>, String> getShiftTypeById() {
			return shiftTypeById;
		}
	}

	public ShiftEfficiencyTracker(String mode) {
        this.mode = mode;
        this.revenueByShift = new HashMap<>();
		this.shiftByRequest = new HashMap<>();
		this.finishedShifts = new HashMap<>();
		this.actualStartByShift = new HashMap<>();
		this.actualEndByShift = new HashMap<>();
		this.shiftTypeById = new HashMap<>();
		this.currentRecord = new Record(revenueByShift, shiftByRequest, finishedShifts, actualStartByShift, actualEndByShift, shiftTypeById);
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
            actualStartByShift.put(event.getShiftId(), event.getTime());
            event.getShiftType().ifPresent(type -> shiftTypeById.put(event.getShiftId(), type));
        }
    }

    @Override
    public void handleEvent(DrtShiftEndedEvent event) {
        if(event.getMode().equals(mode)) {
            activeShifts.remove(event.getVehicleId());
            finishedShifts.put(event.getShiftId(), event.getVehicleId());
            actualEndByShift.put(event.getShiftId(), event.getTime());
            // fall back to the ended event's type if the started event carried none (types should match)
            event.getShiftType().ifPresent(type -> shiftTypeById.putIfAbsent(event.getShiftId(), type));
        }
    }

    @Override
    public void reset(int iteration) {
        this.revenueByShift = new HashMap<>();
        this.shiftByRequest = new HashMap<>();
        this.finishedShifts = new HashMap<>();
        this.actualStartByShift = new HashMap<>();
        this.actualEndByShift = new HashMap<>();
        this.shiftTypeById = new HashMap<>();
		this.currentRecord = new Record(revenueByShift, shiftByRequest, finishedShifts, actualStartByShift, actualEndByShift, shiftTypeById);
        this.activeShifts.clear();
    }

	public Record getCurrentRecord() {
		return currentRecord;
	}
}
