/* *********************************************************************** *
 * project: org.matsim.*
 * OptionManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.gregor.withindayevac.analyzer;

import java.util.HashMap;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;

public class OptionManager {
	
	private final HashMap<Id,NextLinkOption> linkOptions = new HashMap<Id,NextLinkOption>();
	
	public void addNextLinkOptions(NextLinkOption option) {
		this.linkOptions.put(option.getNextLink().getId(), option);
	}
	
	public void updateNextLinkOption(Link link, double conf) {
		
	}
	
	public void reset() {
		this.linkOptions.clear();
	}
	

}
