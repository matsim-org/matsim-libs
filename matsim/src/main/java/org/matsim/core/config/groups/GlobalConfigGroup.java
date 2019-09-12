/* *********************************************************************** *
 * project: org.matsim.*
 * GlobalConfigGroup.java
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

package org.matsim.core.config.groups;

import java.util.Map;

import javax.validation.constraints.PositiveOrZero;

import org.apache.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;

public final class GlobalConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = Logger.getLogger(GlobalConfigGroup.class);

	public static final String GROUP_NAME = "global";

	private boolean insistingOnDeprecatedConfigVersion = true ;
	// yyyy this should be set to false eventually.  kai, aug'18

	public GlobalConfigGroup() {
		super(GROUP_NAME);
	}

	private static final String RANDOM_SEED = "randomSeed";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String COORDINATE_SYSTEM = "coordinateSystem";

	private long randomSeed = 4711L;
	@PositiveOrZero
	private int numberOfThreads = 2;
	private String coordinateSystem = "Atlantis" ;
	// see https://matsim.atlassian.net/browse/MATSIM-898

	@Override
	public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(NUMBER_OF_THREADS, "\"global\" number of threads.  "
				+ "This number is used, e.g., for replanning, but NOT in the mobsim.  "
				+ "This can typically be set to as many cores as you have available, or possibly even slightly more.") ;
		return map ;
	}

	/* direct access */

	@StringGetter( RANDOM_SEED )
	public long getRandomSeed() {
		return this.randomSeed;
	}
	@StringSetter( RANDOM_SEED )
	public void setRandomSeed(final long randomSeed) {
		this.randomSeed = randomSeed;
	}

	@StringGetter( NUMBER_OF_THREADS )
	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}
	@StringSetter( NUMBER_OF_THREADS )
	public void setNumberOfThreads(final int numberOfThreads) {
		log.info("setting number of threads to: " + numberOfThreads ) ; // might not be so bad to do this everywhere?  benjamin/kai, oct'10
		this.numberOfThreads = numberOfThreads;
	}

	@StringGetter( COORDINATE_SYSTEM )
	public String getCoordinateSystem() {
		return this.coordinateSystem;
	}
	@StringSetter( COORDINATE_SYSTEM )
	public void setCoordinateSystem(final String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

	private static final String INSITING_ON_DEPRECATED_CONFIG_VERSION = "insistingOnDeprecatedConfigVersion" ;
	@StringGetter( INSITING_ON_DEPRECATED_CONFIG_VERSION )
	public final boolean isInsistingOnDeprecatedConfigVersion() { return this.insistingOnDeprecatedConfigVersion ; }
	@StringSetter( INSITING_ON_DEPRECATED_CONFIG_VERSION )
	public final void setInsistingOnDeprecatedConfigVersion( boolean val ) {
		this.insistingOnDeprecatedConfigVersion = val ;
	}

}
