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
package playground.thibautd.socnetsim.framework.controller;

import org.matsim.core.controler.AbstractModule;

import playground.thibautd.socnetsim.replanning.grouping.DynamicGroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;

/**
 * @author thibautd
 */
public class SocialNetworkModule extends AbstractModule {

	@Override
	public void install() {
		// TODO Auto-generated method stub
		bind( GroupIdentifier.class ).to( DynamicGroupIdentifier.class );
	}
}

