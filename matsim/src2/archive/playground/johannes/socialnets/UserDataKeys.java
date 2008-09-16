/* *********************************************************************** *
 * project: org.matsim.*
 * UserDataKeys.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnets;

import edu.uci.ics.jung.utils.UserDataContainer;

/**
 * @author illenberger
 *
 */
public interface UserDataKeys {

//	public static final String PERSON_KEY = "person";
	
	public static final String ID = "person_id";
	
	public static final String X_COORD = "x";
	
	public static final String Y_COORD = "y";
	
//	public static final String WAVE_KEY = "wave";
	
	public static final String SAMPLED_KEY = "sampled";
	
	public static final String DETECTED_KEY = "detected";
	
//	public static final String PARTICIPATE_KEY = "participate";
	
	public static final String ANONYMOUS_KEY = "anonymous";
	
	public static final String SAMPLE_PROBA_KEY = "sampleprobability";
	
	public static final String TYPE_KEY = "type";
	
	public static final UserDataContainer.CopyAction.Shared COPY_ACT = new UserDataContainer.CopyAction.Shared();

}
