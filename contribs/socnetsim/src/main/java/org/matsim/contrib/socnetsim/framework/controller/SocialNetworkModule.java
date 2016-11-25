/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.controller;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.contrib.socnetsim.framework.SocialNetworkConfigGroup;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;
import org.matsim.core.controler.AbstractModule;

import org.matsim.contrib.socnetsim.framework.replanning.grouping.DynamicGroupIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupIdentifier;

/**
 * @author thibautd
 */
public class SocialNetworkModule extends AbstractModule {

	@Override
	public void install() {
		bind( GroupIdentifier.class ).to( DynamicGroupIdentifier.class );
	}

	@Provides @Singleton
	public SocialNetwork provideSocialNetwork( final SocialNetworkConfigGroup conf ) {
		return new SocialNetworkReader().read( conf.getInputFile() );
	}
}

