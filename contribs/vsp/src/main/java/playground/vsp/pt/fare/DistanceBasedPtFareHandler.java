package playground.vsp.pt.fare;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.PtConstants;

import java.util.HashMap;
import java.util.Map;

public class DistanceBasedPtFareHandler implements ActivityStartEventHandler {
    @Inject
    private EventsManager events;

    private final double minFare;
    private final double shortTripIntercept;
    private final double shortTripSlope;
    private final double longTripIntercept;
    private final double longTripSlope;
    private final double longTripThreshold;

    private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
    private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();

    public DistanceBasedPtFareHandler(DistanceBasedPtFareParams params) {
        this.minFare = params.getMinFare();
        this.shortTripIntercept = params.getNormalTripIntercept();
        this.shortTripSlope = params.getNormalTripSlope();
        this.longTripIntercept = params.getLongDistanceTripIntercept();
        this.longTripSlope = params.getLongDistanceTripSlope();
        this.longTripThreshold = params.getLongDistanceTripThreshold();
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

                double fare = computeFare(distance, longTripThreshold, minFare, shortTripIntercept, shortTripSlope, longTripIntercept, longTripSlope);
                // charge fare to the person
                events.processEvent(
                        new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare,
                                PtFareConfigGroup.PT_FARE, DistanceBasedPtFareParams.PT_FARE_DISTANCE_BASED, event.getPersonId().toString()));

                personDepartureCoordMap.remove(personId);
                personArrivalCoordMap.remove(personId);
            }
        }
    }

    public static double computeFare(double distance, double longTripThreshold, double minFare,
                                     double shortTripIntercept, double shortTripSlope,
                                     double longTripIntercept, double longTripSlope) {
        if (distance <= longTripThreshold) {
            return Math.max(minFare, shortTripIntercept + shortTripSlope * distance);
        } else {
            return Math.max(minFare, longTripIntercept + longTripSlope * distance);
        }
    }

    @Override
    public void reset(int iteration) {
        personArrivalCoordMap.clear();
        personDepartureCoordMap.clear();
    }
}
