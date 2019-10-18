package vwExamples.utils.createShiftingScenario.cityCommuterDRT;

import java.util.Map;
import java.util.StringJoiner;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.geotools.MGC;

import vwExamples.utils.createShiftingScenario.SubTourValidator;

public class isCityCommuterTourCandidate implements SubTourValidator {
	Network network;
	Map<String, Geometry> cityZonesMap;
	Map<String, Geometry> serviceAreazonesMap;

	public isCityCommuterTourCandidate(Network network, Map<String, Geometry> cityZonesMap, Map<String, Geometry> serviceAreazonesMap) {
		this.network = network;
		this.cityZonesMap = cityZonesMap;
		this.serviceAreazonesMap = serviceAreazonesMap;

	}

	@Override
	public boolean isValidSubTour(Subtour subTour) {

		if (((isInboundCommuterTour(subTour) || isOutboundCommuterTour(subTour) || isWithinCommuterTour(subTour)))) {

			return true;
		}

		else
			return false;
	}

	public double getBeelineTourLength(Subtour subTour) {
		double distance = 0;
		for (Trip trip : subTour.getTrips()) {

			Coord fromCoord = trip.getOriginActivity().getCoord();
			Coord toCoord = trip.getDestinationActivity().getCoord();

			distance = distance + DistanceUtils.calculateDistance(fromCoord, toCoord);

		}
		return distance;
	}

	public boolean isWithinCityTourCheck(Subtour subtour) {
		for (Trip trip : subtour.getTrips()) {
			Coord fromCoord = trip.getOriginActivity().getCoord();
			Coord toCoord = trip.getDestinationActivity().getCoord();

			if (!isWithinZone(fromCoord) || !isWithinZone(toCoord)) {
				return false;
			}

		}
		return true;
	}
	public String getSubtourActivityChain(Subtour subtour) {
		StringJoiner joiner = new StringJoiner("-");

		for (Trip trip : subtour.getTrips()) {
			String act = trip.getOriginActivity().getType();

			if (act.contains("home")) {
				act = "home";
			} else if (act.contains("work")) {
				act = "work";
			} else if (act.contains("shopping")) {
				act = "shopping";
			} else if (act.contains("other")) {
				act = "other";
			} else if (act.contains("leisure")) {
				act = "leisure";
			}

			joiner.add(act);
		}
		// Finalize with last act

		joiner.add("home");

		return joiner.toString();
	}

	public boolean isInboundCommuterTour(Subtour subTour) {

		if (livesOutside(subTour) && worksInside(subTour)) {
			return true;
		}
		return false;

	}

	public boolean isOutboundCommuterTour(Subtour subTour) {

		if (worksOutside(subTour) && livesInside(subTour)) {
			return true;
		}
		return false;

	}

	public boolean isWithinCommuterTour(Subtour subTour) {

		if (worksInside(subTour) && livesInside(subTour)) {
			return true;
		}
		return false;

	}

	public boolean worksInside(Subtour subTour) {

		for (Trip trip : subTour.getTrips()) {
			String ActBefore = trip.getOriginActivity().getType();

			if (ActBefore.contains("work")) {
				Link fromLink = network.getLinks().get(trip.getOriginActivity().getLinkId());

				Coord coord = fromLink.getCoord();
				// If work is inside zoneMap return true
				if (isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean worksOutside(Subtour subTour) {

		for (Trip trip : subTour.getTrips()) {
			String ActBefore = trip.getOriginActivity().getType();

			if (ActBefore.contains("work")) {
				Link fromLink = network.getLinks().get(trip.getOriginActivity().getLinkId());

				Coord coord = fromLink.getCoord();
				// If work is inside zoneMap return true
				if (!isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean livesInside(Subtour subTour) {

		for (Trip trip : subTour.getTrips()) {
			String ActBefore = trip.getOriginActivity().getType();

			if (ActBefore.contains("home")) {
				Link fromLink = network.getLinks().get(trip.getOriginActivity().getLinkId());

				Coord coord = fromLink.getCoord();
				// If work is inside zoneMap return true
				if (isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean livesOutside(Subtour subTour) {

		for (Trip trip : subTour.getTrips()) {
			String ActBefore = trip.getOriginActivity().getType();

			if (ActBefore.contains("home")) {
				Link fromLink = network.getLinks().get(trip.getOriginActivity().getLinkId());

				Coord coord = fromLink.getCoord();
				// If work is inside zoneMap return true
				if (!isWithinZone(coord)) {
					return true;
				}

			}

		}

		return false;

	}

	public boolean subTourIsWithinServiceArea(Subtour subtour) {

		for (Trip trip : subtour.getTrips()) {
			Coord ActCoord = trip.getDestinationActivity().getCoord();

			for (String partOfServiceAreaId : serviceAreazonesMap.keySet()) {
				Geometry partOfServiceAreaGeo = serviceAreazonesMap.get(partOfServiceAreaId);

				if (!partOfServiceAreaGeo.intersects(MGC.coord2Point(ActCoord))) {
					// System.out.println("Coordinate in "+ zone);
					return false;
				}

			}

		}
		return true;

	}

	public boolean isWithinZone(Coord coord) {
		// Function assumes Shapes are in the same coordinate system like MATSim
		// simulation

		for (String zone : cityZonesMap.keySet()) {
			Geometry geometry = cityZonesMap.get(zone);
			if (geometry.intersects(MGC.coord2Point(coord))) {
				// System.out.println("Coordinate in "+ zone);
				return true;
			}
		}

		return false;
	}

}