/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.mapping.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.filter.NetworkLinkFilter;
import playground.polettif.publicTransitMapping.tools.MiscUtils;

import java.util.Set;

/**
 * A Link filter to separate links by allowed transport modes.
 */
public class LinkFilterMode implements NetworkLinkFilter {

	private final Set<String> modes;

	public LinkFilterMode(Set<String> modes) {
		this.modes = modes;
	}

	@Override
	public boolean judgeLink(Link l) {
		return MiscUtils.setsShareMinOneStringEntry(l.getAllowedModes(), modes);
	}
}
