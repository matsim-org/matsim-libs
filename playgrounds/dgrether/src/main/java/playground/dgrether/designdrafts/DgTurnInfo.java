/* *********************************************************************** *
 * project: org.matsim.*
 * DgTurnInfo
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
package playground.dgrether.designdrafts;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;


/**
 * @author dgrether
 *
 */
public class DgTurnInfo {
	
		private final Id fromLinkId;
		private Set<String> modes;
		
		public DgTurnInfo(final Id fromLinkId) {
			this.fromLinkId = fromLinkId;
		}
		
		public Id getFromLinkId() {
			return this.fromLinkId;
		}
		
		public Set<Id> getToLinkIdsAllModes() {
			return null;
		}
		
		public Set<Id> getToLinkIds(TransportMode mode){
			return null;
		}
		
		public void addToLink(TransportMode mode){
			
		}
		
		
}
