// code by jph
package playground.clruch.io.fleet;

import org.matsim.api.core.v01.Coord;

import playground.clruch.dispatcher.core.AVStatus;
import playground.clruch.dispatcher.core.RequestStatus;

// TODO remove Coord, do with Tensor
public class TaxiStamp {
	public AVStatus avStatus;
	public RequestStatus requestStatus;
	public int requestIndex;
	public Coord gps;
}
