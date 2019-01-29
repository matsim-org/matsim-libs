/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertorConfigGroup.java
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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;


/**
 * @author thibautd
 */
public class JointTripInsertorConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "jointTripInsertor";

	private List<String> chainBasedModes = Arrays.asList( TransportMode.car , TransportMode.bike );
	private double betaDetour = 2;
	private double scale = 1;

	public JointTripInsertorConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "chainBasedModes" )
	public String getStringChainBasedModes() {
		if ( chainBasedModes.isEmpty() ) return "";

		final StringBuffer b = new StringBuffer( chainBasedModes.get( 0 ) );
		for ( int i=1; i < chainBasedModes.size(); i++ ) {
			b.append( ',' );
			b.append( chainBasedModes.get( i ) );
		}

		return b.toString();
	}

	public List<String> getChainBasedModes() {
		return this.chainBasedModes;
	}

	@StringGetter( "betaDetour" )
	public double getBetaDetour() {
		return this.betaDetour;
	}

	@StringSetter( "betaDetour" )
	public void setBetaDetour(final String betaDetour) {
		this.betaDetour = Double.parseDouble( betaDetour );
	}

	@StringGetter( "scale" )
	public double getScale() {
		return this.scale;
	}

	@StringSetter( "scale" )
	public void setScale(final String scale) {
		this.scale = Double.parseDouble( scale );
	}

	@StringSetter( "chainBasedModes" )
	public void setChainBasedModes(final String chainBasedModes) {
		this.chainBasedModes = Arrays.asList( chainBasedModes.split(",") );
	}
}

