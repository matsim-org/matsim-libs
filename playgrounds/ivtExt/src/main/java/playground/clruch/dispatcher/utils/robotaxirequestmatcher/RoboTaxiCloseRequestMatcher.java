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
import playground.clruch.dispatcher.core.RoboTaxi;
import playground.clruch.dispatcher.utils.PlaneEuclideanDistance;
import playground.clruch.dispatcher.utils.PlaneLocation;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** This matcher iterates through the supplied collection of robotaxis and assigns to
 * each taxi the closest request.
 * 
 * Attention: the order of the robotaxis supplied changes the result.
 * 
 * @author Claudio Ruch */
public class RoboTaxiCloseRequestMatcher implements AbstractRoboTaxiRequestMatcher {
    private Set<AVRequest> requestsMatched;

    @Override
    public final void match(List<RoboTaxi> robotaxis, Collection<AVRequest> requests, //
            BiConsumer<RoboTaxi, AVRequest> matchingFunction) {

        requestsMatched = new HashSet<>();

        for (RoboTaxi robotaxi : robotaxis) {
            if (requests.size() > 0 && requestsMatched.size() < requests.size()) {
                AVRequest avr = findClosest(robotaxi, requests);
                if (avr != null) {
                    boolean matched = requestsMatched.add(avr);
                    GlobalAssert.that(matched);
                    matchingFunction.accept(robotaxi, avr);
                }
            }
        }
    }

    // TODO implement efficiently using some tree structure. 
    private AVRequest findClosest(RoboTaxi robotaxi, Collection<AVRequest> requests) {
        double distCloest = Double.MAX_VALUE;
        AVRequest closest = null;

        Tensor locRoboTaxi = PlaneLocation.of(robotaxi);
        for (AVRequest avr : requests) {
            if (!requestsMatched.contains(avr)) {
                Tensor locAVR = PlaneLocation.of(avr);
                double dist = PlaneEuclideanDistance.of(locRoboTaxi, locAVR);
                if (dist < distCloest) {
                    distCloest = dist;
                    closest = avr;
                }

            }
        }
        return closest;
    }

}
