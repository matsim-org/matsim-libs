package playground.clruch.dispatcher.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import playground.clruch.dispatcher.core.VehicleLinkPair;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/**
 * OldestRequestSelector orders {@link AVRequest} according to the submission time.
 */
public class OldestRequestSelector extends AbstractRequestSelector {

    @Override
    public Collection<AVRequest> selectRequests( //
            Collection<VehicleLinkPair> vehicleLinkPairs, //
            Collection<AVRequest> avRequests, int size) {
        return avRequests.stream() //
                .sorted((r1, r2) -> Double.compare(r1.getSubmissionTime(), r2.getSubmissionTime())) //
                .limit(size) //
                .collect(Collectors.toList());
    }

    /**
     * the purpose of the main function is to demonstrate the ordering when using
     * Double::compare
     */
    public static void main(String[] args) {
        List<Double> list = new ArrayList<>();
        list.add(7.);
        list.add(2.);
        list.add(4.);
        Collections.sort(list, new Comparator<Double>() {
            @Override
            public int compare(Double d1, Double d2) {
                return Double.compare(d1, d2);
            }
        });
        System.out.println(list);
    }

}
