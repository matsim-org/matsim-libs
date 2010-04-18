package playground.mmoyo.ptRouterAdapted;

import org.matsim.pt.router.TransitRouterConfig;

public class MyTransitRouterConfig extends TransitRouterConfig {

	/**
	 * The distance in meters in which stop facilities should be searched for
	 * around the start and end coordinate.
	 */
	public double searchRadius = 1000.0;
	
	/**
	 * If no stop facility is found around start or end coordinate (see 
	 * {@link #searchRadius}), the nearest stop location is searched for
	 * and the distance from start/end coordinate to this location is
	 * extended by the given amount.<br />
	 * If only one stop facility is found within {@link #searchRadius},
	 * the radius is also extended in the hope to find more stop
	 * facilities (e.g. in the opposite direction of the already found
	 * stop). 
	 */
	public double extensionRadius = 200.0;
	
	/**
	 * The distance in meters that agents can walk to get from one stop to 
	 * another stop of a nearby transit line.
	 */
	public double beelineWalkConnectionDistance = 100.0;
	
	/**
	 * Walking speed of agents on transfer links, beeline distance.
	 */
	public double beelineWalkSpeed = 3.0/3.6;  // presumably, in m/sec.  3.0/3.6 = 3000/3600 = 3km/h.  kai, apr'10
	
	public double marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
	
	public double marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0; // in Eu/sec; includes opportunity cost of time.  kai, apr'10
	
	public double marginalUtilityOfTravelDistanceTransit = -0.0;    // yyyy presumably, in Eu/m ?????????  so far, not used.  kai, apr'10
	
	/**
	 * The additional costs to be added when an agent switches lines.
	 */
	public double costLineSwitch = 60.0 * -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle  // in Eu.  kai, apr'10
	

}
