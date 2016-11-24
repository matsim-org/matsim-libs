/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.parking.parkingchoice.lib.obj;

import java.util.HashMap;

import org.apache.log4j.Logger;

public class CollectionLib {

	private static final Logger log = Logger.getLogger(CollectionLib.class);

	/**
	 * TODO: move method to approporaite place where the data structures are
	 * located.
	 * 
	 * @param hm
	 */
	public static void printHashmapToConsole(HashMap hm) {
		for (Object key : hm.keySet()) {
			if (key == null) {
				System.out.println("null" + "\t" + hm.get(key));
			} else {
				System.out.println(key.toString() + "\t" + hm.get(key));
			}
		}
	}

	public static void logHashmap(HashMap hm) {
		for (Object key : hm.keySet()) {
			if (key == null) {
				log.info("null" + "\t" + hm.get(key));
			} else {
				log.info(key.toString() + "\t" + hm.get(key));
			}
		}
	}

}
