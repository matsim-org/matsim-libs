// code by clruch and jph
package playground.clruch.dispatcher.utils.robotaxirequestmatcher;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public interface AbstractRoboTaxiRequestMatcher {
    void match(List<RoboTaxi> robotaxis, Collection<AVRequest> requests, //
            BiConsumer<RoboTaxi, AVRequest> matchingFunction);

}
