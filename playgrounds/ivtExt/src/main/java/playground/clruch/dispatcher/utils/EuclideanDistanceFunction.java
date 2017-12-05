/**
 * 
 */
package playground.clruch.dispatcher.utils;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

import ch.ethz.matsim.av.passenger.AVRequest;
import playground.clruch.dispatcher.core.RoboTaxi;

/**
 * @author Claudio Ruch
 *
 */
public class EuclideanDistanceFunction implements DistanceFunction {


    @Override
    public double getDistance(RoboTaxi robotaxi, AVRequest avrequest) {
        
        return CoordUtils.calcEuclideanDistance(robotaxi.getDivertableLocation().getCoord(), 
                avrequest.getFromLink().getCoord());
    }


    @Override
    public double getDistance(RoboTaxi robotaxi, Link link) {
        
        return CoordUtils.calcEuclideanDistance(robotaxi.getDivertableLocation().getCoord(), 
                link.getCoord());
        
    }


	@Override
	public double getDistance(Link from, Link to) {
		// TODO Nicolo Added -- needs to be checked
		  return CoordUtils.calcEuclideanDistance(from.getCoord(), 
				  to.getCoord());
	}

}
