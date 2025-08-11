package org.matsim.drtExperiments.utils;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;

public class DrtOperationUtils {

    public static GeneralRequest createFromDrtRequest(DrtRequest drtRequest) {
        return new GeneralRequest( drtRequest.getPassengerIds().get( 0 ), drtRequest.getFromLink().getId(),
                drtRequest.getToLink().getId(), drtRequest.getEarliestStartTime(), drtRequest.getLatestStartTime(),
                drtRequest.getConstraints().latestArrivalTime() );
    }

}
