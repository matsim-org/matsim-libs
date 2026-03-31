package ch.sbb.matsim.routing.pt.raptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.*;

/**
 * Modifies the original transit schedule ids.
 */
class ScrambleTransitSchedule {


	/**
	 * Creates a new transit schedule with all ids (facility, line, route) replaced by randomly generated ones.
	 * The input schedule is not modified.
	 *
	 * @param schedule The original transit schedule
	 * @return A new transit schedule with scrambled ids
	 */
	public static TransitSchedule scramble(TransitSchedule schedule) {
		// Create new schedule and factory
		TransitSchedule newSchedule = schedule.getFactory().createTransitSchedule();
		TransitScheduleFactory factory = newSchedule.getFactory();

		long counter = 1;

		// Map to store the relationship between old and new facility IDs
		Map<Id<TransitStopFacility>, Id<TransitStopFacility>> facilityIdMap = new HashMap<>();
		Map<Id<TransitLine>, Id<TransitLine>> lineIdMap = new HashMap<>();
		Map<Id<TransitRoute>, Id<TransitRoute>> routeIdMap = new HashMap<>();

		// Process transit stop facilities
		for (TransitStopFacility facility : schedule.getFacilities().values()) {
			// Create new ID for the facility
			Id<TransitStopFacility> newId = Id.create("f" + counter++, TransitStopFacility.class);

			// Create a new facility with the same attributes but new ID
			TransitStopFacility newFacility = factory.createTransitStopFacility(
					newId,
					facility.getCoord(),
					facility.getIsBlockingLane());

			// Copy link ID if it exists
			if (facility.getLinkId() != null) {
				newFacility.setLinkId(facility.getLinkId());
			}

			// Copy name if it exists
			if (facility.getName() != null) {
				newFacility.setName(facility.getName());
			}

			// Store the mapping
			facilityIdMap.put(facility.getId(), newId);

			// Add to new schedule
			newSchedule.addStopFacility(newFacility);
		}

		// Process transit lines
		for (TransitLine line : schedule.getTransitLines().values()) {
			// Create new ID for the line
			Id<TransitLine> newLineId = Id.create("l" + counter++, TransitLine.class);

			// Store line ID mapping
			lineIdMap.put(line.getId(), newLineId);

			// Create a new line with new ID
			TransitLine newLine = factory.createTransitLine(newLineId);

			// Copy name if it exists
			if (line.getName() != null) {
				newLine.setName(line.getName());
			}

			// Process transit routes for this line
			for (TransitRoute route : line.getRoutes().values()) {
				// Create new ID for the route
				Id<TransitRoute> newRouteId = Id.create("r" + counter++, TransitRoute.class);

				// Store route ID mapping
				routeIdMap.put(route.getId(), newRouteId);

				// Create list of transit route stops with replaced facility IDs
				List<TransitRouteStop> newStops = new ArrayList<>();
				for (TransitRouteStop stop : route.getStops()) {
					Id<TransitStopFacility> newFacilityId = facilityIdMap.get(stop.getStopFacility().getId());
					TransitStopFacility newFacility = newSchedule.getFacilities().get(newFacilityId);

					TransitRouteStop newStop = factory.createTransitRouteStop(
							newFacility,
							stop.getArrivalOffset().orElse(0),
							stop.getDepartureOffset().orElse(0));
					newStop.setAwaitDepartureTime(stop.isAwaitDepartureTime());
					newStops.add(newStop);
				}

				// Create a new route
				TransitRoute newRoute = factory.createTransitRoute(
						newRouteId,
						route.getRoute(),
						newStops,
						route.getTransportMode());

				// Copy description if it exists
				if (route.getDescription() != null) {
					newRoute.setDescription(route.getDescription());
				}

				// Copy departures
				for (Departure departure : route.getDepartures().values()) {
					Departure newDeparture = factory.createDeparture(
							departure.getId(),
							departure.getDepartureTime());

					if (departure.getVehicleId() != null) {
						newDeparture.setVehicleId(departure.getVehicleId());
					}

					newDeparture.setChainedDepartures(departure.getChainedDepartures());
					newRoute.addDeparture(newDeparture);
				}

				// Add route to line
				newLine.addRoute(newRoute);
			}

			// Add line to schedule
			newSchedule.addTransitLine(newLine);
		}

		for (TransitLine line : newSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure d : route.getDepartures().values()) {
					d.setChainedDepartures(
						d.getChainedDepartures().stream()
							.map(c -> factory.createChainedDeparture(
								lineIdMap.get(c.getChainedTransitLineId()),
								routeIdMap.get(c.getChainedRouteId()),
								c.getChainedDepartureId()
							)).toList()
					);
				}
			}
		}

		MinimalTransferTimes.MinimalTransferTimesIterator it = schedule.getMinimalTransferTimes().iterator();
		while (it.hasNext()) {
			it.next();
			newSchedule.getMinimalTransferTimes().set(
				facilityIdMap.get(it.getFromStopId()),
				facilityIdMap.get(it.getToStopId()),
				it.getSeconds()
			);
		}

		return newSchedule;
	}
}
