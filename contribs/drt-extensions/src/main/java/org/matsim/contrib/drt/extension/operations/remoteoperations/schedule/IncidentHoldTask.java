/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.evrp.ETask;

import java.util.Collections;
import java.util.Map;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

/**
 * A vehicle hold caused by a remote-guidance incident: the (autonomous) vehicle has run into a situation it cannot
 * resolve on its own and must wait in place until a remote operator has processed the incident. Unlike shift breaks,
 * changeovers or vehicle services, an incident hold happens <em>wherever the vehicle currently is on the network</em> —
 * there is no physical {@link org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility} to
 * reserve. It therefore implements only the {@link OperationalStop} marker (NOT
 * {@link org.matsim.contrib.drt.extension.operations.shifts.schedule.FacilityStop}), which is enough for the standard
 * DRT insertion logic to leave the vehicle alone while it is held.
 * <p>
 * As a {@link STOP}-base {@link DrtStopTask} with empty pickup/dropoff sets, the standard
 * {@link org.matsim.contrib.drt.vrpagent.DrtActionCreator} turns it into a holding stop activity that simply waits
 * until {@link #getEndTime()}. Passenger boarding/alighting is not possible during a hold, so the request mutators
 * throw; the {@code calc*} time-window methods return permissive bounds (the hold's timing is defined solely by its
 * begin/end times), mirroring {@code WaitForShiftTask}.
 * <p>
 * Like the other operations stay-type tasks ({@code WaitForShiftTask}, {@code ShiftBreakTaskImpl}), this is a
 * <em>unified</em> task usable in both the standard and the electric (eDRT) fleet: it implements {@link ETask} directly
 * so the eDRT energy bookkeeping (which casts every scheduled task to {@link ETask}) works without a separate electric
 * subclass. A held vehicle is stationary, so there is no drive energy; only time-dependent auxiliary consumption over
 * the hold accrues. The consumed energy is supplied by the creator (the incident dispatcher), which knows the vehicle's
 * auxiliary consumption model; it defaults to 0 for the non-electric case, where the value is simply ignored.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentHoldTask extends DefaultStayTask implements DrtStopTask, ETask {

	public static final DrtTaskType TYPE = new DrtTaskType("INCIDENT_HOLD", STOP);

	private final double consumedEnergy;

	public IncidentHoldTask(double beginTime, double endTime, Link link) {
		this(beginTime, endTime, link, 0.0);
	}

	public IncidentHoldTask(double beginTime, double endTime, Link link, double consumedEnergy) {
		super(TYPE, beginTime, endTime, link);
		this.consumedEnergy = consumedEnergy;
	}

	@Override
	public double getTotalEnergy() {
		return consumedEnergy;
	}

	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getDropoffRequests() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getPickupRequests() {
		return Collections.emptyMap();
	}

	@Override
	public void addDropoffRequest(AcceptedDrtRequest request) {
		throw new UnsupportedOperationException("Passenger dropoff is not possible during an incident hold");
	}

	@Override
	public void addPickupRequest(AcceptedDrtRequest request) {
		throw new UnsupportedOperationException("Passenger pickup is not possible during an incident hold");
	}

	@Override
	public void removePickupRequest(Id<Request> requestId) {
		throw new UnsupportedOperationException("Passenger pickup is not possible during an incident hold");
	}

	@Override
	public void removeDropoffRequest(Id<Request> requestId) {
		throw new UnsupportedOperationException("Passenger dropoff is not possible during an incident hold");
	}

	@Override
	public double calcEarliestArrivalTime() {
		return 0;
	}

	@Override
	public double calcLatestArrivalTime() {
		return Double.MAX_VALUE;
	}

	@Override
	public double calcEarliestDepartureTime() {
		return 0;
	}

	@Override
	public double calcLatestDepartureTime() {
		return Double.MAX_VALUE;
	}
}