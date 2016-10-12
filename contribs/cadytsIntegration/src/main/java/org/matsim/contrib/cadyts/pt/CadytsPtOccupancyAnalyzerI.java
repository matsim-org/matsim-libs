package org.matsim.contrib.cadyts.pt;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface CadytsPtOccupancyAnalyzerI extends TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	int getOccupancyVolumeForStopAndTime(Id<TransitStopFacility> stopId, int time_s);

	void writeResultsForSelectedStopIds(String filename, Counts<Link> occupCounts, Collection<Id<TransitStopFacility>> stopIds);

	int[] getOccupancyVolumesForStop(Id<TransitStopFacility> stopId);

}