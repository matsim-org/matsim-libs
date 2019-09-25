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
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface CadytsPtOccupancyAnalyzerI extends TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	int getOccupancyVolumeForStopAndTime(Id<Facility> stopId, int time_s);

	void writeResultsForSelectedStopIds(String filename, Counts<Link> occupCounts, Collection<Id<Facility>> stopIds );

	int[] getOccupancyVolumesForStop(Id<Facility> stopId);

}
