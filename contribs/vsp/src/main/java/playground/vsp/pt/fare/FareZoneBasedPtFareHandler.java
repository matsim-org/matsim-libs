package playground.vsp.pt.fare;

import com.google.inject.Inject;
import org.apache.commons.math.stat.regression.SimpleRegression;
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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FareZoneBasedPtFareHandler implements ActivityStartEventHandler {
	@Inject
	private EventsManager events;

	public static final String FARE = "fare";
	public static final String PT_FARE_ZONE_BASED = "fare zone based pt fare";
	public static final String PT_GERMANWIDE_FARE_BASED = "german-wide fare based pt fare";

	private final ShpOptions shp;

	private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
	private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();

	public FareZoneBasedPtFareHandler(DistanceBasedPtFareParams params) {
		this.shp = new ShpOptions(params.getFareZoneShp(), null, null);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {

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

				Map.Entry<String, Double> fareEntry = computeFare(distance, departureZone, arrivalZone);
				// charge fare to the person
				events.processEvent(
					new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fareEntry.getValue(),
						PtFareConfigGroup.PT_FARE, fareEntry.getKey(), event.getPersonId().toString()));

				personDepartureCoordMap.remove(personId);
				personArrivalCoordMap.remove(personId);
			}
		}
	}

	public static Map.Entry<String, Double> computeFare(double distance, SimpleFeature departureZone, SimpleFeature arrivalZone) {

		if (departureZone != null && arrivalZone != null) {
//			if both zones are not null -> departure and arrival point are inside of one of the tarifzonen
			if (departureZone.getID().equals(arrivalZone.getID())) {
				return new AbstractMap.SimpleEntry<>(PT_FARE_ZONE_BASED ,(double) departureZone.getAttribute(FARE));
			}
		}
//		in every other case return german wide fare / Deutschlandtarif
		return getGermanWideFare(distance);
	}

	private static Map.Entry<String, Double> getGermanWideFare(double distance) {

		SimpleRegression regression = new SimpleRegression();

//		in Deutschlandtarif, the linear function for the prices above 100km seem to have a different steepness
//		hence the following difference in data points
//		prices taken from https://deutschlandtarifverbund.de/wp-content/uploads/2024/07/20231201_TBDT_J_10_Preisliste_V07.pdf
		if (distance / 1000 <= 100.) {
			regression.addData(1, 1.70);
			regression.addData(2,1.90);
			regression.addData(3,2.00);
			regression.addData(4,2.10);
			regression.addData(5,2.20);
			regression.addData(6,3.20);
			regression.addData(7,3.70);
			regression.addData(8,3.80);
			regression.addData(9,3.90);
			regression.addData(10,4.10);
			regression.addData(11,5.00);
			regression.addData(12,5.40);
			regression.addData(13,5.60);
			regression.addData(14,5.80);
			regression.addData(15,5.90);
			regression.addData(16,6.40);
			regression.addData(17,6.50);
			regression.addData(18,6.60);
			regression.addData(19,6.70);
			regression.addData(20,6.90);
			regression.addData(30,9.90);
			regression.addData(40,13.70);
			regression.addData(50,16.30);
			regression.addData(60,18.10);
			regression.addData(70,20.10);
			regression.addData(80,23.20);
			regression.addData(90,26.20);
			regression.addData(100,28.10);
		} else {
			regression.addData(100,28.10);
			regression.addData(200,47.20);
			regression.addData(300,59.70);
			regression.addData(400,71.70);
			regression.addData(500,83.00);
			regression.addData(600,94.60);
			regression.addData(700,106.30);
			regression.addData(800,118.20);
			regression.addData(900,130.10);
			regression.addData(1000,141.00);
			regression.addData(1100,148.60);
			regression.addData(1200,158.10);
			regression.addData(1300,169.20);
			regression.addData(1400,179.80);
			regression.addData(1500,190.10);
			regression.addData(1600,201.50);
			regression.addData(1700,212.80);
			regression.addData(1800,223.30);
			regression.addData(1900,233.90);
			regression.addData(2000,244.00);
		}
		return new AbstractMap.SimpleEntry<>(PT_GERMANWIDE_FARE_BASED, regression.getSlope() * distance / 1000 + regression.getIntercept());
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
