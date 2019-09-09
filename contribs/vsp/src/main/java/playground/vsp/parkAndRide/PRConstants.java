package playground.vsp.parkAndRide;

import org.matsim.core.api.internal.MatsimParameters;

/**
 * An abstract class containing some constants used for park-and-ride.
 * 
 * @author ikaddoura
 */
public abstract class PRConstants implements MatsimParameters {

	/**
	* An activity connecting a car leg and a pt leg (i.e. a transit_walk leg)
	* 
	*/
	public final static String PARKANDRIDE_ACTIVITY_TYPE = "parkAndRide";

}
