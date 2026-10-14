package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Optional;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShift extends Identifiable<DrtShift>, Comparable<DrtShift> {

	double getStartTime();

	double getEndTime();

	boolean isStarted();

	boolean isEnded();

	void start();

	void end();

	Optional<Id<OperationFacility>> getOperationFacilityId();

	Optional<DrtShiftBreak> getBreak();

	Optional<Id<DvrpVehicle>> getDesignatedVehicleId();

	Optional<String> getShiftType();

	/**
	 * Whether this shift's {@link #getEndTime() end} is a hard, pre-committed deadline that must be materialised into
	 * the vehicle schedule up front (a changeover + wait-for-shift tail plus a landing reservation), as
	 * {@code ShiftTaskSchedulerImpl.startShift} does for driver shifts. This eager tail is load-bearing there: it
	 * guarantees a hub landing slot for the mandatory (legal working-time) end, and — because the materialised
	 * changeover is an insertion waypoint anchored at the shift end — it enforces the return-to-hub-by-shift-end
	 * boundary on every prebooking insertion.
	 * <p>
	 * Returns {@code false} for shifts whose end is <em>discretionary</em> (no fixed deadline; e.g. remote-guidance
	 * virtual shifts that run to the simulation horizon and are ended on demand by a recall). For those, materialising
	 * the tail is pointless (the horizon-anchored changeover/reservation guard nothing) and actively harmful (it forces
	 * a spurious end-of-day deadhead to a hub and adds the rigidity a recall then has to fight). Such shifts stay in
	 * service on a plain stay until recalled; the end is materialised lazily by the dispatcher's early-end mechanism.
	 *
	 * @return {@code true} (default) for a fixed, mandatory end; {@code false} for a discretionary, lazily-materialised end
	 */
	default boolean hasCommittedEnd() {
		return true;
	}

	static Id<DrtShift> id(String id) {
		return Id.create(id, DrtShift.class);
	}
}
