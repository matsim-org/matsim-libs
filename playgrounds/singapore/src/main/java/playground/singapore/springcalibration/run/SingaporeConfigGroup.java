/* *********************************************************************** *
 * project: org.matsim.*
 * OTFVisConfig.java
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

package playground.singapore.springcalibration.run;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;


/**
 * A config module for Singapore
 *
 * @author anhorni
 *
 */
public class SingaporeConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "singapore";

	public static final String TAXI_WAITING_TIMES_FILE = "taxi_waitingtimes_file";
	public static final String VALIDATION_PATH = "validation_path";
	
	private String taxiWaitingTimeFile = "";
	private String validationPath = "";
	
	public SingaporeConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public String getValue(final String key) {
		// for variables that have getters, this is not needed (and should probably be avoided).  kai, jan'11
		if (TAXI_WAITING_TIMES_FILE.equals(key)) {
			return this.taxiWaitingTimeFile;
		} 
		else if (VALIDATION_PATH.equals(key)) {
			return this.validationPath;
		} else {
			throw new IllegalArgumentException(key + ".  There may exist a direct getter.");
		}
	}

	@Override
	public void addParam(final String key, final String value) {
		// this is needed since config file parsing uses it.
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
		
		if (TAXI_WAITING_TIMES_FILE.equals(key)) {
			this.taxiWaitingTimeFile = value;
		} 
		else if (VALIDATION_PATH.equals(key)) {
			this.validationPath = value;
		}
		else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		// this is needed for everything since the config dump is based on this.
		
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(TAXI_WAITING_TIMES_FILE, this.getTaxiWaitingTimeFile());
		map.put(VALIDATION_PATH, this.getValidationPath());
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(TAXI_WAITING_TIMES_FILE, "Taxi waiting times from shape file");
		map.put(VALIDATION_PATH, "Path to folder containing HITS validation analyses");
		return map ;
	}

	/* direct access */

	public void setTaxiWaitingTimeFile(final String taxiWaitingTimeFile) {
		this.taxiWaitingTimeFile = taxiWaitingTimeFile;
	}

	public String getTaxiWaitingTimeFile() {
		return this.taxiWaitingTimeFile;
	}

	public String getValidationPath() {
		return this.validationPath;
	}

	public void setValidationPath(String validationPath) {
		this.validationPath = validationPath;
	}


}
