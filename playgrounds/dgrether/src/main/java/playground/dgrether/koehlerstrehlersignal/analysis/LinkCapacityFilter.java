/* *********************************************************************** *
 * project: org.matsim.*
 * LinkCapacityFilter
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.analysis;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.filter.NetworkLinkFilter;


/**
 * @author dgrether
 *
 */
public class LinkCapacityFilter implements NetworkLinkFilter {

	private boolean isSmaller;
	private double flowCapacityPerHour;
	
	public LinkCapacityFilter(double flowCapacityPerHour, boolean isSmaller){
		this.isSmaller = isSmaller;
		this.flowCapacityPerHour = flowCapacityPerHour;
	}
	
	@Override
	public boolean judgeLink(Link l) {
		if (l.getCapacity() > this.flowCapacityPerHour) {
			if (! isSmaller) {
				return true;
			}
			else {
				return false;
			}
		}
		return isSmaller;
	}

}
