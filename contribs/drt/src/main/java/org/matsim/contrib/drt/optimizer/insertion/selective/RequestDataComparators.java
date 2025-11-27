package org.matsim.contrib.drt.optimizer.insertion.selective;

import org.matsim.contrib.drt.optimizer.insertion.parallel.partitioner.RequestData;

import java.util.Comparator;

public class RequestDataComparators {
    public static final Comparator<RequestData> REQUEST_DATA_COMPARATOR = Comparator
        .comparingDouble((RequestData r) ->
            r.getSolution().insertion().get().detourTimeInfo.getTotalTimeLoss())
        .thenComparing(r -> r.getDrtRequest().getId().toString());
}
