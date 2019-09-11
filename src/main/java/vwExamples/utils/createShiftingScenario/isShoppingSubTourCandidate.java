package vwExamples.utils.createShiftingScenario;

import java.util.Map;
import java.util.StringJoiner;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.geometry.geotools.MGC;

public class isShoppingSubTourCandidate implements SubTourValidator {
	Network network;
	Map<String, Geometry> cityZonesMap;
	Map<String, Geometry> serviceAreazonesMap;

	isShoppingSubTourCandidate(Network network, Map<String, Geometry> cityZonesMap,
			Map<String, Geometry> serviceAreazonesMap) {
		this.network = network;
		this.cityZonesMap = cityZonesMap;
		this.serviceAreazonesMap = serviceAreazonesMap;


	}

	@Override
	public boolean isValidSubTour(Subtour subTour) {
//		boolean subTourInServiceArea = subTourIsWithinServiceArea(subTour);
//		String chain = getSubtourActivityChain(subTour);
//		String requiredChain = "home-work-home"; //Umlegungslogik bedingt triviale Wegekette
		
//		String requiredChain = "work"; //Muss ein Arbeiter sein, Dämpfungsfaktor, weil davon nicht alle 6 %

//		if ((isInboundCommuterTour(subTour) || isOutboundCommuterTour(subTour) || isWithinCommuterTour(subTour))
//				&& subTourInServiceArea && chain.equals(requiredChain)) {
//			return true;
//		}
		
		if (isWithinCityShoppingTour(subTour)) {
	
			return true;
		}
				

		else return false;
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


	
	public boolean isWithinCityShoppingTour(Subtour subTour) {

		if (shopInside(subTour) && livesInside(subTour)) {
			return true;
		}
		return false;

	}

	public boolean shopInside(Subtour subTour) {

		for (Trip trip : subTour.getTrips()) {
			String ActBefore = trip.getOriginActivity().getType();

			if (ActBefore.contains("shopping")) {
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