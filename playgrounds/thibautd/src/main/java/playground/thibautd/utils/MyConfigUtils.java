/* *********************************************************************** *
 * project: org.matsim.*
 * MyConfigUtils.java
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
package playground.thibautd.utils;

import java.util.Map;

import org.matsim.core.config.ConfigGroup;

/**
 * @author thibautd
 */
public class MyConfigUtils {
	public static <T extends ConfigGroup> void transmitParams(
			final T source,
			final T target) {
		for ( Map.Entry<String, String> e : source.getParams().entrySet() ) {
			target.addParam( e.getKey() , e.getValue() );
		}
	}
}

