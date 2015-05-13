/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsMutatorConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.framework.cliques.config;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;

/**
 * A config group for the joint trips mutator.
 * @author thibautd
 */
public class JointTripsMutatorConfigGroup extends ConfigGroup {
	private static final Logger log =
		Logger.getLogger(JointTripsMutatorConfigGroup.class);

	public static enum SelectorName {
		EXP_BETA, BEST_SCORE, RANDOM, SELECTED;
	}

	public static final String GROUP_NAME = "JointTripsMutator";

	public static final String START_PROB = "startMutationProbability";
	public static final String END_PROB = "endMutationProbability";
	public static final String SELECTOR = "planSelector";

	private double startProb = 0.6d;
	private double endProb = 0.1d;
	private SelectorName selector = SelectorName.EXP_BETA;

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public JointTripsMutatorConfigGroup() {
		super( GROUP_NAME );
	}

	// /////////////////////////////////////////////////////////////////////////
	// base class methods
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public void addParam(
			final String param_name,
			final String value) {
		// emulate previous behavior of reader (ignore null values at reading). td Apr'15
		if ( "null".equalsIgnoreCase( value ) ) return;
	
		if (param_name.equals( START_PROB )) {
			setStartMutationProbability( value );
		}
		else if (param_name.equals( END_PROB )) {
			setEndMutationProbability( value );
		}
		else if (param_name.equals( SELECTOR )) {
			setSelector( value );
		}
		else {
			log.warn( "unknown parameter "+param_name );
		}
	}

	@Override
	public String getValue(
			final String param_name) {
		if (param_name.equals( START_PROB )) {
			return ""+getStartMutationProbability();
		}
		else if (param_name.equals( END_PROB )) {
			return ""+getEndMutationProbability();
		}
		else if (param_name.equals( SELECTOR )) {
			return ""+getSelector();
		}
		return null;
	}

	@Override
	public TreeMap<String,String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();

		addParameterToMap( map , START_PROB );
		addParameterToMap( map , END_PROB );
		addParameterToMap( map , SELECTOR );
		return map;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters/setters
	// /////////////////////////////////////////////////////////////////////////
	private void setStartMutationProbability(final String value) {
		try {
			double p = Double.parseDouble( value );

			if (p >= 0 && p <= 1) {
				startProb = p;
			}
			else {
				log.warn( "invalid probability "+p+": keeping start mutation probability to "+startProb );
			}
		}
		catch (NumberFormatException e) {
			log.warn( "invalid number "+value+": keeping start mutation probability to "+startProb );
		}
	}

	private void setEndMutationProbability(final String value) {
		try {
			double p = Double.parseDouble( value );

			if (p >= 0 && p <= 1) {
				endProb = p;
			}
			else {
				log.warn( "invalid probability "+p+": keeping end mutation probability to "+endProb );
			}
		}
		catch (NumberFormatException e) {
			log.warn( "invalid number "+value+": keeping end mutation probability to "+endProb );
		}
	}

	public double getStartMutationProbability() {
		return startProb;
	}

	public double getEndMutationProbability() {
		return endProb;
	}

	private void setSelector(final String value) {
		try {
			SelectorName name = SelectorName.valueOf( value.toUpperCase().trim() );
			this.selector = name;
		}
		catch (IllegalArgumentException e) {
			log.warn( "invalid selector name: "+value.toUpperCase().trim() );
			log.warn( "keeping current value "+selector );
		}
	}

	public SelectorName getSelector() {
		return selector;
	}
}

