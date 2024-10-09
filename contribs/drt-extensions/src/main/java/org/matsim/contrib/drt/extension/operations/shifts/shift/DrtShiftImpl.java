package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Optional;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class DrtShiftImpl implements DrtShift {

	private final Id<DrtShift> id;

	private final double start;
	private final double end;
	private final Id<OperationFacility> operationFacilityId;
	private final Id<DvrpVehicle> designatedVehicleId;

	private final DrtShiftBreak shiftBreak;

	private boolean started = false;
	private boolean ended = false;

	public DrtShiftImpl(Id<DrtShift> id, double start, double end, Id<OperationFacility> operationFacilityId,
						Id<DvrpVehicle> designatedVehicleId, DrtShiftBreak shiftBreak) {
		this.id = id;
		this.start = start;
		this.end = end;
		this.operationFacilityId = operationFacilityId;
        this.designatedVehicleId = designatedVehicleId;
        this.shiftBreak = shiftBreak;
	}

	@Override
	public double getStartTime() {
		return start;
	}

	@Override
	public double getEndTime() {
		return end;
	}

	@Override
	public Optional<DrtShiftBreak> getBreak() {
		return Optional.ofNullable(shiftBreak);
	}

	@Override
	public Optional<Id<DvrpVehicle>> getDesignatedVehicleId() {
		return Optional.ofNullable(designatedVehicleId);
	}

	@Override
	public boolean isStarted() {
		return started;
	}

	@Override
	public boolean isEnded() {
		return ended;
	}

	@Override
	public void start() {
		if(!started) {
			started = true;
		} else {
			throw new IllegalStateException("Shift already started!");
		}
	}

	@Override
	public void end() {
		if(!ended) {
			ended = true;
		} else {
			throw new IllegalStateException("Shift already ended!");
		}
	}

	@Override
	public Optional<Id<OperationFacility>> getOperationFacilityId() {
		return Optional.ofNullable(operationFacilityId);
	}

	@Override
	public Id<DrtShift> getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Shift " + id.toString() + " ["+start+"-"+end+"]";
	}

	@Override
	public int compareTo(DrtShift shift) {
		return this.id.compareTo(shift.getId());
	}
}
