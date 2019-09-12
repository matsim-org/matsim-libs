/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
* @author ikaddoura
*/

public final class AccidentsContext {
	// class is public so it can be bound from outside package

	@Inject AccidentsContext(){}
	// injected constructor is package-private so that nobody can instationate this class directly
	
	private Map<Id<Link>, AccidentLinkInfo> linkId2info = new HashMap<>();

	Map<Id<Link>, AccidentLinkInfo> getLinkId2info() {
		return linkId2info;
	}

}

