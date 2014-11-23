/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.tmp;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CheckIdInstance {
	final private static Logger LOG = Logger.getLogger(CheckIdInstance.class);
	public static void main(String[] args) {
		
		Id<Link> linkId = Id.createLinkId("1");
		Object o = linkId;

		Id<?> someId = null;
		if(o instanceof Id<?>){
			someId = (Id<?>)o;
		}
		
		@SuppressWarnings("unchecked")
		Id<Link> newId = (Id<Link>) someId; 
		
		
	}

}
