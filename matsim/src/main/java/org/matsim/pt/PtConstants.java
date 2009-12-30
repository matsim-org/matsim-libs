package org.matsim.pt;

import org.matsim.api.core.v01.population.Activity;

/**
 * An abstract class containing some constants used for public transportation.
 * 
 * @author mrieser
 */
public abstract class PtConstants {

	/**
	 * Type of an activity that somehow interacts with pt, e.g. to connect a walk leg
	 * to a pt leg, or to connect two pt legs together where agents have to change lines.
	 * 
	 * @see Activity#setType(String)
	 */
	public final static String TRANSIT_ACTIVITY_TYPE = "pt interaction";

}
