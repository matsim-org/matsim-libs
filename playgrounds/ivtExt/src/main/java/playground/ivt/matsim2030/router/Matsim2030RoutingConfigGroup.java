/* *********************************************************************** *
 * project: org.matsim.*
 * Matsim2030RoutingConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.ivt.matsim2030.router;

import org.matsim.core.config.experimental.ReflectiveModule;

/**
 * @author thibautd
 */
public class Matsim2030RoutingConfigGroup extends ReflectiveModule {
	public static String GROUP_NAME = "matsim2030routing";

	// Having a path hard-coded as a default value is short from causing me a heart
	// attack, but it is still much better than what I found while diving in,
	// where the path was hard-coded IN THE CONSTRUCTOR OF THE TRANSIT ROUTER FACTORY
	private String thinnedTransitRouterNetworkFile = "./input/run1/thinned_uvek2005network_adjusted.xml.gz";

	public Matsim2030RoutingConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "thinnedTransitRouterNetworkFile" )
	public String getThinnedTransitRouterNetworkFile() {
		return this.thinnedTransitRouterNetworkFile;
	}

	@StringSetter( "thinnedTransitRouterNetworkFile" )
	public void setThinnedTransitRouterNetworkFile(
			final String thinnedTransitRouterNetworkFile) {
		this.thinnedTransitRouterNetworkFile = thinnedTransitRouterNetworkFile;
	}

}

