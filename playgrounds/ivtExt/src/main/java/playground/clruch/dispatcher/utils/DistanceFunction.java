/**
 * 
 */
package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;

import playground.clruch.dispatcher.core.RoboTaxi;
import ch.ethz.matsim.av.passenger.AVRequest;

/** @author Claudio Ruch */
public interface DistanceFunction {
    double getDistance(RoboTaxi robotaxi, AVRequest avRequest);

    double getDistance(RoboTaxi robotaxi, Link link);
    
    // Added Nicolo 29-10-17
    double getDistance(Link from, Link to);
}
