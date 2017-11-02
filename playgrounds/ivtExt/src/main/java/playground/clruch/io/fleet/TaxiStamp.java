// code by jph
package playground.clruch.io.fleet;

import org.matsim.api.core.v01.Coord;

import playground.clruch.dispatcher.core.AVStatus;

// TODO Coord could be our own implementation with 2 Scalars for x,y and a toTensor function
public class TaxiStamp {
	public AVStatus avStatus;
	public Coord gps;
}
