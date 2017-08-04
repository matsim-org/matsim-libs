// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.stream.Collectors;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * OldestRequestSelector orders {@link AVRequest} according to the submission time.
 */
public class OldestRequestSelector extends AbstractRequestSelector {

    @Override
    public Collection<AVRequest> selectRequests( //
            Collection<RoboTaxi> vehicleLinkPairs, //
            Collection<AVRequest> avRequests, int size) {
        return avRequests.stream() //
                .sorted((r1, r2) -> Double.compare(r1.getSubmissionTime(), r2.getSubmissionTime())) //
                .limit(size) //
                .collect(Collectors.toList());
    }
}
