/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.parkAndRide;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.config.ConfigGroup;

/**
 * Additional parameters which are required by the park-and-ride module.
 * 
 * @author ikaddoura
 *
 */
public class PRConfigGroup extends ConfigGroup{

	public static final String GROUP_NAME = "parkAndRide";
	private static final String INPUT_FILE= "inputFile";
	private static final String GRAVITY = "gravity";
	private static final String INTERMODAL_TRANSFER_PENALTY = "intermodalTransferPenalty";
	private static final String TYPICAL_DURATION = "typicalDuration";
	
	private String inputFile = null;
	private double gravity = 2;
	private double intermodalTransferPenalty = 0.;
	private double typicalDuration = 120.;
	
	public PRConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public void addParam(final String key, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
		
		if (INPUT_FILE.equals(key)) {
			setInputFile(value);
		} else if (GRAVITY.equals(key)) {
			setGravity(Double.parseDouble(value));
		} else if (INTERMODAL_TRANSFER_PENALTY.equals(key)) {
			setIntermodalTransferPenalty(Double.parseDouble(value));
		} else if (TYPICAL_DURATION.equals(key)) {
			setTypicalDuration(Double.parseDouble(value));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public String getValue(final String key) {
		if (INPUT_FILE.equals(key)) {
			return getInputFile();
		} else if (GRAVITY.equals(key)) {
			return Double.toString(getGravity());
		} else if (INTERMODAL_TRANSFER_PENALTY.equals(key)) {
			return Double.toString(getIntermodalTransferPenalty());
		} else if (TYPICAL_DURATION.equals(key)) {
			return Double.toString(getTypicalDuration());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(INPUT_FILE, getValue(INPUT_FILE));
		map.put(INTERMODAL_TRANSFER_PENALTY, getValue(INTERMODAL_TRANSFER_PENALTY));
		map.put(GRAVITY, getValue(GRAVITY));
		map.put(TYPICAL_DURATION, getValue(TYPICAL_DURATION));
		return map;
	}
	
	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		
		map.put(INPUT_FILE, "REQUIRED: a csv file with park-and-ride facilities. Format: park-and-ride-facility-Id;Link1in-Id;Link1out-Id;Link2in-Id;Link2out-Id;Link3in-Id;Link3out-Id;TransitStopFacilityName;Capacity[vehicles per park-and-ride-facility]");
		
		map.put(INTERMODAL_TRANSFER_PENALTY, "[utils/transfer] normally negative utility for park-and-ride transfers");

		map.put(GRAVITY, "facility choice parameter: park-and-ride facilities are chosen following a simple gravitation model: " + "\n\t\t" +
				" for each agent and for all park-and-ride facilities a weight is calculated. the weights determine the probability to be chosen. " + "\n\t\t" + 
				" the weight is calculated based on the beeline (home -> park-and-ride facility -> work) and the gravitation parameter as follows: " + "\n\t\t" + 
				" WEIGHT = 1 / (BEELINE ^ GRAVITY)" );
		
		return map;
	}

	public String getInputFile() {
		return inputFile;
	}

	public void setInputFile(String inputFile) {
		this.inputFile = inputFile;
	}

	public double getGravity() {
		return gravity;
	}

	public void setGravity(double gravity) {
		this.gravity = gravity;
	}

	public double getIntermodalTransferPenalty() {
		return intermodalTransferPenalty;
	}

	public void setIntermodalTransferPenalty(double intermodalTransferPenalty) {
		this.intermodalTransferPenalty = intermodalTransferPenalty;
	}

	public double getTypicalDuration() {
		return typicalDuration;
	}

	public void setTypicalDuration(double typicalDuration) {
		this.typicalDuration = typicalDuration;
	}
	
}
