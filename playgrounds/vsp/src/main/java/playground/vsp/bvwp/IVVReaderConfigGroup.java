/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.vsp.bvwp;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author nagel
 *
 */
public class IVVReaderConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "IVVReaderConfigGroup" ;
	
	// ---
	private static final String TRAVEL_TIMES_FILE = "travelTimesMatrixFile" ;
	private String travelTimesMatrixFile ;
	// ---
	// next entry
	// ---
	
	/**
	 * @param name
	 */
	public IVVReaderConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * @param name
	 * @param storeUnknownParametersAsStrings
	 */
	public IVVReaderConfigGroup( boolean storeUnknownParametersAsStrings) {
		super(GROUP_NAME, storeUnknownParametersAsStrings);
	}
	@StringGetter(TRAVEL_TIMES_FILE)
	public String getTravelTimesMatrixFile() {
		return travelTimesMatrixFile;
	}
	@StringSetter(TRAVEL_TIMES_FILE)
	public void setTravelTimesMatrixFile(String travelTimesMatrixFile) {
		this.travelTimesMatrixFile = travelTimesMatrixFile;
	}

}
