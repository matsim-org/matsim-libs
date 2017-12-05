/**
 * 
 */
package playground.clruch.dispatcher.utils.robotaxirequestmatcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import ch.ethz.idsc.owly.data.GlobalAssert;
import ch.ethz.idsc.tensor.Tensor;
import ch.ethz.matsim.av.passenger.AVRequest;
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.PlaneEuclideanDistance;
import playground.clruch.dispatcher.utils.PlaneLocation;

/** @author Claudio Ruch */
// TODO doc
public class RequestCloseRoboTaxiMatcher implements AbstractRoboTaxiRequestMatcher {
    private Set<RoboTaxi> roboTaxisMatched;

    @Override
    public final void match(List<RoboTaxi> robotaxis, Collection<AVRequest> requests, //
            BiConsumer<RoboTaxi, AVRequest> matchingFunction) {

        roboTaxisMatched = new HashSet<>();

        for (AVRequest avr : requests) {
            if (requests.size() > 0 && roboTaxisMatched.size() < requests.size()) {
                RoboTaxi robotaxi = findClosest(avr, robotaxis);
                if (robotaxi != null) {
                    boolean matched = roboTaxisMatched.add(robotaxi);
                    GlobalAssert.that(matched);
                    matchingFunction.accept(robotaxi, avr);
                }

            }
        }

    }

    // TODO implement efficiently using some tree structure.
    private RoboTaxi findClosest(AVRequest avr, List<RoboTaxi> robotaxis) {
        double distClosest = Double.MAX_VALUE;
        RoboTaxi closest = null;

        Tensor locAVR = PlaneLocation.of(avr);
        for (RoboTaxi robotaxi : robotaxis) {
            if (!roboTaxisMatched.contains(robotaxi)) {
                Tensor locRT = PlaneLocation.of(robotaxi);
                double dist = PlaneEuclideanDistance.of(locRT, locAVR);
                if (dist < distClosest) {
                    distClosest = dist;
                    closest = robotaxi;
                }

            }
        }
        return closest;
    }

}
