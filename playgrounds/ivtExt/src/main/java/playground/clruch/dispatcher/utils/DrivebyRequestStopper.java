// code by clruch and jph
package playground.clruch.dispatcher.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** @author Claudio Ruch
 *
 *         call with setVehiclePickup as biConsumer, this class stops vehicles which are diving by
 *         an open request. */
public enum DrivebyRequestStopper {
    ;

    public static int stopDrivingBy(Map<Link, List<AVRequest>> requestLocs, Collection<RoboTaxi> roboTaxis, BiConsumer<RoboTaxi, AVRequest> biConsumer) {
        int numDriveByPickup = 0;

        for (RoboTaxi roboTaxi : roboTaxis) {
            Link link = roboTaxi.getDivertableLocation();
            if (requestLocs.containsKey(link)) {
                List<AVRequest> requestList = requestLocs.get(link);
                if (!requestList.isEmpty()) {
                    AVRequest request = requestList.get(0);
                    biConsumer.accept(roboTaxi, request);
                    ++numDriveByPickup;
                }
            }
        }
        return numDriveByPickup;
    }
}
