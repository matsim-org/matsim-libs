package org.matsim.core.mobsim.qsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Data class holding information about a passenger relocating at one stop to reach the next destination.
 */
public record PTPassengerRelocation(Id<TransitStopFacility> stop, Id<TransitStopFacility> nextDestination) {
}
