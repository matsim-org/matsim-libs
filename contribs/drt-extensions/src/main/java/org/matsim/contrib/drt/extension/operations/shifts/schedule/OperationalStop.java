package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.contrib.dvrp.schedule.Task;

/**
 * Marker interface for tasks that represent an <em>operational</em> stop of a vehicle, i.e. a stop that is part of
 * operating the service rather than serving a passenger request (shift breaks, shift changeovers, waiting for a shift,
 * vehicle services, remote-guidance incident holds, ...). Its sole cross-cutting semantics is: while a vehicle performs
 * (or is scheduled to perform) an operational stop, the standard DRT insertion logic must leave it alone — no passenger
 * requests are inserted into it.
 * <p>
 * Operational stops that additionally reserve a physical
 * {@link org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility} (and may charge there)
 * carry that through the {@link FacilityStop} sub-interface. Operational stops that happen wherever the vehicle
 * currently is on the network (e.g. an incident hold) implement only this marker.
 *
 * @author nkuehnel / MOIA
 */
public interface OperationalStop extends Task {
}