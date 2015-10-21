package org.matsim.pt;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.api.internal.MatsimParameters;

/**
 * An abstract class containing some constants used for public transportation.
 * 
 * @author mrieser
 */
public abstract class PtConstants implements MatsimParameters {

	/**
	 * Type of an activity that somehow interacts with pt, e.g. to connect a walk leg
	 * to a pt leg, or to connect two pt legs together where agents have to change lines.
	 * 
	 * @see Activity#setType(String)
	 */
	public final static String TRANSIT_ACTIVITY_TYPE = "pt interaction";
	
	// this is currently used for wait2link events where the mode is not clear (bus, rail...?!), theresa sep'2015
	public final static String NETWORK_MODE = "pt unspecified";
	

}
