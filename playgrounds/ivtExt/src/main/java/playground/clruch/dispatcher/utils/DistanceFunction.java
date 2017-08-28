/**
 * 
 */
package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.RoboTaxi;
import playground.sebhoerl.avtaxi.passenger.AVRequest;

/** @author Claudio Ruch */
public interface DistanceFunction {
    double getDistance(RoboTaxi robotaxi, AVRequest avRequest);

    double getDistance(RoboTaxi robotaxi, Link link);
}
