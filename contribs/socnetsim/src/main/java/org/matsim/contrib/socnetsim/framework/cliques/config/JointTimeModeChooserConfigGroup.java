/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeModeChooserConfigGroup.java
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
package org.matsim.contrib.socnetsim.framework.cliques.config;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigGroup;

/**
 * @author thibautd
 */
public class JointTimeModeChooserConfigGroup extends ConfigGroup {
	public static final String GROUP_NAME = "jointTimeModeChooser";

	//parameter names
	private static final String STEPS = "durationSteps";
	private static final String MODES = "availableModes";
	private static final String DEBUG = "debugMode";
	private static final String NEGATIVE_PENALTY = "negativeDurationPenalty";
	private static final String UNSYNCHRO_PENALTY = "unsynchronizedPenalty";

	//parameter values
	private List<Integer> steps = Arrays.asList( 1 * 60 , 5 * 60 , 25 * 60 , 125 * 60 , 625 * 60 );
	private List<String> modes = Arrays.asList( TransportMode.car , TransportMode.pt , TransportMode.walk , TransportMode.bike );
	private boolean debug = false;
	private double negativeDurationPenalty = 100;
	private double unsynchronizedPenalty = 1E-5;

	public JointTimeModeChooserConfigGroup() {
		super(GROUP_NAME);
	}

	/*
	 * =========================================================================
	 * base class methods
	 * =========================================================================
	 */
	@Override
	public void addParam(final String param_name, final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
	
		if (param_name.equals( STEPS )) {
			setDurationSteps(value);
		}
		else if (param_name.equals( MODES )) {
			setModes( value );
		}
		else if (param_name.equals( DEBUG )) {
			setDebugMode( value );
		}
		else if (param_name.equals( NEGATIVE_PENALTY )) {
			setNegativeDurationPenalty( value );
		}
		else if (param_name.equals( UNSYNCHRO_PENALTY )) {
			setUnsynchronizedPenalty( value );
		}
	}

	@Override
	public String getValue(final String param_name) {
		if (param_name.equals( STEPS )) {
			return toString( steps );
		}
		else if (param_name.equals( MODES )) {
			return toString( modes );
		}
		else if (param_name.equals( DEBUG )) {
			return ""+isDebugMode();
		}
		else if (param_name.equals( NEGATIVE_PENALTY )) {
			return ""+getNegativeDurationPenalty();
		}
		else if (param_name.equals( UNSYNCHRO_PENALTY )) {
			return ""+getUnsynchronizedPenalty();
		}
		return null;
	}

	@Override
	public TreeMap<String,String> getParams() {
		TreeMap<String,String> map = new TreeMap<String,String>();
		this.addParameterToMap(map, STEPS);
		this.addParameterToMap(map, MODES);
		this.addParameterToMap(map, DEBUG);
		this.addParameterToMap(map, NEGATIVE_PENALTY);
		this.addParameterToMap(map, UNSYNCHRO_PENALTY);
		return map;
	}

	/*
	 * =========================================================================
	 * getters/setters
	 * =========================================================================
	 */

	public List<Integer> getDurationSteps() {
		return this.steps;
	}

	public void setDurationSteps(final String value) {
		steps = string2Ints( value );
	}

	public List<String> getModes() {
		return modes;
	}

	public void setModes(final String value) {
		modes = string2Strings( value );
	}

	public boolean isDebugMode() {
		return debug;
	}

	public void setDebugMode(final String value) {
		if ("true".equals( value.toLowerCase().trim() )) {
			debug = true;
		}
		else {
			debug = false;
		}
	}


	private void setUnsynchronizedPenalty(final String value) {
		unsynchronizedPenalty = Double.parseDouble( value );
	}

	public double getUnsynchronizedPenalty() {
		return unsynchronizedPenalty;
	}

	private void setNegativeDurationPenalty(final String value) {
		negativeDurationPenalty = Double.parseDouble( value );
	}

	public double getNegativeDurationPenalty() {
		return negativeDurationPenalty;
	}

	private static List<Integer> string2Ints(final String value) {
		String[] strings = value.split(",");
		Integer[] integers = new Integer[ strings.length ];

		for (int i=0; i < strings.length; i++) {
			integers[ i ] = Integer.valueOf( strings[ i ].trim() );
		}

		return Arrays.asList( integers );
	}

	private static List<String> string2Strings(final String value) {
		String[] strings = value.split(",");

		for (int i=0; i < strings.length; i++) {
			strings[ i ] = strings[ i ].trim();
		}

		return Arrays.asList( strings );
	}

	private static String toString( List<? extends Object> list ) {
		StringBuffer buff = new StringBuffer();

		for (Object o : list) {
			buff.append( o.toString()+"," );
		}

		String s = buff.toString();
		return s.substring( 0 , s.length() - 1 );
	}
}
