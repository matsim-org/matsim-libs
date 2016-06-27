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

package playground.polettif.publicTransitMapping.config;

import java.util.Collections;
import java.util.Set;

/**
 * Contains Strings that are used throughout the package
 */
public final class PublicTransitMappingStrings {

	/**
	 * Suffix used for child stop facilities. The id of the referenced link is appended (i.e. stop0123.link:7852).
	 */
	public static final String SUFFIX_CHILD_STOP_FACILITIES = ".link:";
	public static final String SUFFIX_CHILD_STOP_FACILITIES_REGEX = "[.]link:";

	public static final String ARTIFICIAL_LINK_MODE = "artificial";
	public static final Set<String> ARTIFICIAL_LINK_MODE_AS_SET = Collections.singleton(ARTIFICIAL_LINK_MODE);
	public static final String STOP_FACILITY_LOOP_LINK = "stopFacilityLink";
}
