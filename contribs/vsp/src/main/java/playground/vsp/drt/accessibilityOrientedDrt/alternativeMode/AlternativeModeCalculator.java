package playground.vsp.drt.accessibilityOrientedDrt.alternativeMode;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.List;

public class AlternativeModeCalculator {
    private final SwissRailRaptor raptor;
    private final TravelTime travelTime;
    private final TravelDisutility travelDisutility;
    private final LeastCostPathCalculator router;


    public AlternativeModeCalculator(SwissRailRaptor raptor, Network network, TravelTime travelTime, TravelDisutility travelDisutility) {
        this.raptor = raptor;
        this.travelTime = travelTime;
        this.travelDisutility = travelDisutility;
        this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
    }

    public AlternativeModeCalculator(SwissRailRaptor raptor, Network network) {
        this.raptor = raptor;
        this.travelTime = new QSimFreeSpeedTravelTime(1);
        this.travelDisutility = new TimeAsTravelDisutility(travelTime);
        this.router = new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
    }

    public AlternativeModeTripData calculateAlternativeTripData(DrtRequest drtRequest) {
        return calculateAlternativeTripData(drtRequest.getPassengerIds().get(0).toString(), drtRequest.getFromLink(),
                drtRequest.getToLink(), drtRequest.getEarliestStartTime());
    }

    public AlternativeModeTripData calculateAlternativeTripData(String tripId, Link fromLink, Link toLink, double departureTime) {
        double directCarTravelTime = VrpPaths.calcAndCreatePath(fromLink, toLink, departureTime, router, travelTime).getTravelTime();

        Coord fromCoord = fromLink.getToNode().getCoord();
        Coord toCoord = toLink.getToNode().getCoord();
        List<? extends PlanElement> legs = raptor.calcRoute(DefaultRoutingRequest.withoutAttributes
                (new LinkWrapperFacility(fromLink), new LinkWrapperFacility(toLink), departureTime, null));

        double actualTotalTravelTime = 0;
        double totalWalkDistance = 0;
        String mode;

        if (legs == null) {
            // No route can be found -> walk as alternative mode
            double euclideanDistance = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
            double walkingDistance = euclideanDistance * 1.3;
            // This is the default value from config file.
            // TODO Consider read it from config directly.
            actualTotalTravelTime = walkingDistance / 0.8333333333333333;
            totalWalkDistance = walkingDistance;
            mode = TransportMode.walk;
        } else if (legs.size() == 1) {
            // Direct walk is faster
            Leg walking = (Leg) legs.get(0);
            actualTotalTravelTime = walking.getTravelTime().seconds();
            totalWalkDistance = walking.getRoute().getDistance();
            mode = TransportMode.walk;
        } else {
            // A suitable PT route is found (usually it is walk-pt-walk, walk-pt-pt-walk, walk-pt-walk-pt-walk, ...)
            double arrivalTime = Double.NaN;
            for (PlanElement planElement : legs) {
                Leg leg = (Leg) planElement;
                if (leg.getMode().equals(TransportMode.walk)) {
                    totalWalkDistance += leg.getRoute().getDistance();
                }
                arrivalTime = leg.getDepartureTime().seconds() + leg.getTravelTime().seconds();
//                actualTotalTravelTime += leg.getTravelTime().seconds();
            }
            mode = TransportMode.pt;
            actualTotalTravelTime = arrivalTime - departureTime;
        }

        return new AlternativeModeTripData(tripId, departureTime, fromCoord, toCoord, directCarTravelTime, actualTotalTravelTime, mode, totalWalkDistance);
    }
}
