package playground.vsp.pt.fare;

import com.google.inject.Inject;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.PtConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FareZoneBasedPtFareHandler implements ActivityStartEventHandler {
	@Inject
	private EventsManager events;

	private static final String FARE = "fare";
	public static final String PT_FARE_ZONE_BASED = "fare zone based pt fare";

	private final ShpOptions shp;

	private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
	private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();

//	TODO: integrate this new handler to the upper level infrastructure -> some kind of switch to choose between old distance based handler and this one

	public FareZoneBasedPtFareHandler(ShpOptions shp) {
//		TODO: fill constructor. base fare and shp is needed here
		this.shp = shp;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

//		event sequence:
//		act start pt interaction (at departure) -> act start pt interaction (at arrival) -> act start work / home / whatever
		if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
			personDepartureCoordMap.computeIfAbsent(event.getPersonId(), c -> event.getCoord()); // The departure place is fixed to the place of first pt interaction an agent has in the whole leg
			personArrivalCoordMap.put(event.getPersonId(), event.getCoord()); // The arrival stop will keep updating until the agent start a real activity (i.e. finish the leg)
		}

		if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
			Id<Person> personId = event.getPersonId();
			if (personDepartureCoordMap.containsKey(personId)) {
				double distance = CoordUtils.calcEuclideanDistance
					(personDepartureCoordMap.get(personId), personArrivalCoordMap.get(personId));

				SimpleFeature departureZone = determineFareZone(personDepartureCoordMap.get(personId), shp.readFeatures());
				SimpleFeature arrivalZone = determineFareZone(personArrivalCoordMap.get(personId), shp.readFeatures());

				double fare = computeFare(distance, departureZone, arrivalZone);
				// charge fare to the person
				events.processEvent(
					new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare,
						PtFareConfigGroup.PT_FARE, PT_FARE_ZONE_BASED, event.getPersonId().toString()));

				personDepartureCoordMap.remove(personId);
				personArrivalCoordMap.remove(personId);
			}
		}
	}

	public static double computeFare(double distance, SimpleFeature departureZone, SimpleFeature arrivalZone) {

		if (departureZone != null && arrivalZone != null) {
//			if both zones are not null -> departure and arrival point are inside of one of the tarifzonen
			if (departureZone == arrivalZone) {
				return (double) departureZone.getAttribute(FARE);
			}
		}
//		in every other case return german wide fare or Deutschlandtarif
		return getGermanWideFare(distance);
	}

	private static double getGermanWideFare(double distance) {
//		TODO: find a way to parse deutschlandtarif prices from pdf to csv or manually implement it here as switch case
		double fare = 0.0;

		return fare;
	}

	static SimpleFeature determineFareZone(Coord coord, List<SimpleFeature> features) {
		SimpleFeature zone = null;

		for (SimpleFeature ft : features) {
			Geometry geom = (Geometry) ft.getDefaultGeometry();

			if (MGC.coord2Point(coord).within(geom)) {
				zone = ft;
				break;
			}
		}
		return zone;
	}

	@Override
	public void reset(int iteration) {
		personArrivalCoordMap.clear();
		personDepartureCoordMap.clear();
	}
}
