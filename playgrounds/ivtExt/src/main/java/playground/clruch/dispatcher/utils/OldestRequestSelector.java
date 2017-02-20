package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

public class OldestRequestSelector extends AbstractRequestSelector {

    @Override
    public Collection<AVRequest> selectRequests( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<AVRequest> avRequests, int size) {
        // TODO check if jan can be trusted :-)
        Comparator<AVRequest> comparator = new Comparator<AVRequest>() {

            @Override
            public int compare(AVRequest o1, AVRequest o2) {
                return Double.compare(o1.getSubmissionTime(), o2.getSubmissionTime());
            }

        };
        return avRequests.stream().sorted(comparator).limit(size).collect(Collectors.toList());
    }

}
