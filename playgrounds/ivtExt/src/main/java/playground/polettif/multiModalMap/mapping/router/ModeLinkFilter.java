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


package playground.polettif.multiModalMap.mapping.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.filter.NetworkLinkFilter;

/**
 * Filter to exclude links based on allowedTransportModes
 */
public class ModeLinkFilter implements NetworkLinkFilter {
	
	private final String mode;

	public ModeLinkFilter(String mode) {
		this.mode = mode;
	}

	@Override
	public boolean judgeLink(Link l) {
		return l.getAllowedModes().contains(mode);
	}
}