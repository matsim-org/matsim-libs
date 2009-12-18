/* *********************************************************************** *
 * project: org.matsim.*
 * DgOTFVisUtils
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import org.apache.log4j.Logger;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFConnectionManager.Entry;


/**
 * @author dgrether
 *
 */
public class DgOTFVisUtils {
	
	private static final Logger log = Logger.getLogger(DgOTFVisUtils.class);
	
	public static void printConnectionManager(OTFConnectionManager c) {
		for (Entry e : c.getEntries()){
			log.error("enty from: " + e.getFrom() + " to " + e.getTo());
		}
	}

}
