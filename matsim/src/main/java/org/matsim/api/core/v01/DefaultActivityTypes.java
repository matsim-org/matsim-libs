
/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultActivityTypes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.api.core.v01;

/**
 * In the same way as {@link TransportMode} this is meant to somewhat standardize often used activity types.  Also as in
 * the same way as {@link TransportMode}, the list here is not exhaustive, and can even be completely ignored.
 * <br/>
 * There are loosely based on what is in the Victoria trip survey data ("VISTA"), modified by what was useful in our own
 * experience.
 * <br/>
 * I put the work01 etc in (the numbers referring to their typical duration) since this seems most necessary for work.  Feel free to
 * put other ones with typical durations in if truly needed.  Outside users can (evidently) also program them, in the sense of
 * <pre>
 *     DefaultActivityTypes.
 * </pre>
 */
public final class DefaultActivityTypes {
	private DefaultActivityTypes(){} // do not instantiate
	
	public static final String home = "home" ;

	public static final String work = "work" ;
	
	public static final String work01 = "work01" ;
	public static final String work02 = "work02" ;
	public static final String work03 = "work03" ;
	public static final String work04 = "work04" ;
	public static final String work05 = "work05" ;
	public static final String work06 = "work06" ;
	public static final String work07 = "work07" ;
	public static final String work08 = "work08" ;
	public static final String work09 = "work09" ;
	public static final String work10 = "work10" ;
	public static final String work11 = "work11" ;
	public static final String work12 = "work12" ;
	public static final String work13 = "work13" ;
	public static final String work14 = "work14" ;
	public static final String work15 = "work15" ;
	public static final String work16 = "work16" ;
	
	public static final String personalBusiness = "personalBusiness" ;

	public static final String shopping = "shopping" ;
	
	public static final String leisure = "leisure" ;
	public static final String social = "social" ;

	public static final String pickupOrDropoff = "pickup or dropoff" ;
	public static final String pickupOrDropoffPerson = "pickup or dropoff person" ;
	public static final String pickupOrDropoffItem = "pickup or dropoff item" ;
	
	public static final String unknown = "unknown" ;
	
	/**
	 * Most trip diary surveys are trip oriented.  In consequence, the start with the first trip, which has a purpose,
	 * which is the activity it is going to.  In consequence, the activity type it is coming from is missing.
	 */
	public static final String unknownFirstActivityType = "unknown first activity type" ;
}
