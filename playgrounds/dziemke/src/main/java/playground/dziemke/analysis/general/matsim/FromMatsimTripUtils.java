package playground.dziemke.analysis.general.matsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.Collection;
import java.util.List;

/**
 * @author gthunig on 04.04.2017.
 */
public class FromMatsimTripUtils {
    private static final Logger log = Logger.getLogger(FromMatsimTripUtils.class);

    static double getDurationByCalculation_s(FromMatsimTrip trip){
        return trip.getArrivalTime_s() - trip.getDepartureTime_s();
    }

    static double calculateBeelineDistance_m(FromMatsimTrip trip, Network network) {
        Link departureLink = network.getLinks().get(trip.getDepartureLinkId());
        Link arrivalLink = network.getLinks().get(trip.getArrivalLinkId());

        // TODO use coords of toNode instead of center coord of link
        double arrivalCoordX_m = arrivalLink.getCoord().getX();
        double arrivalCoordY_m = arrivalLink.getCoord().getY();
        double departureCoordX_m = departureLink.getCoord().getX();
        double departureCoordY_m = departureLink.getCoord().getY();

        // TODO use CoordUtils.calcEuclideanDistance instead
        double horizontalDistance_m = Math.abs(departureCoordX_m - arrivalCoordX_m);
        double verticalDistance_m = Math.abs(departureCoordY_m - arrivalCoordY_m);

        return Math.sqrt(horizontalDistance_m * horizontalDistance_m
                + verticalDistance_m * verticalDistance_m);
    }

    static double getDistanceRoutedByCalculation_m(FromMatsimTrip trip, Network network, Collection<String> networkModes) {
        double tripDistance_m = 0.;
        if (trip.getLinks().isEmpty() && networkModes.contains(trip.getLegMode())
                && !trip.getDepartureLinkId().equals(trip.getArrivalLinkId())) {
            log.warn("List of links is empty. LegMode " + trip.getLegMode() + " is listed as NetworkMode.");
        }
        for (int i = 0; i < trip.getLinks().size(); i++) {
            Id<Link> linkId = trip.getLinks().get(i);
            Link link = network.getLinks().get(linkId);
            double length_m = link.getLength();
            tripDistance_m += length_m;
        }
        return tripDistance_m;
        // TODO here, the distances from activity to link and link to activity are missing!
    }


}
