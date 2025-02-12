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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.population.routes.NetworkRoute;

public final class GlobalConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = LogManager.getLogger(GlobalConfigGroup.class);

	public static final String GROUP_NAME = "global";

	public GlobalConfigGroup() {
		super(GROUP_NAME);
	}

	@Override public Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(NUMBER_OF_THREADS, NUMBER_OF_THREADS_CMT ) ;
		return map ;
	}
	// ---
	/**
	 *
	 * @deprecated -- yyyy this needs to move elsewhere.  See discussion points below.  kai, feb'25
	 * <p><ul>
	 *     <li>Since it is determined by the type of the Netsim, it presumably should be gettable from
	 * there?  (If I see this directly, it is currently not settable, which is good, since with that this here just is a first place where to put this
	 * as a global variable.)  kai, feb'25</li>
	 * <li> On the other hand, if it is available from the Netsim, something like {@link
	 * org.matsim.core.population.routes.RouteUtils#calcDistance(NetworkRoute, double, double, Network)} will need the {@link
	 * org.matsim.core.mobsim.qsim.interfaces.Netsim} to be plausibly callable, which is clunky.  It would thus be better to somehow pull if from the
	 * config. There used to be a MobsimConfigGroup which attempted to set some standards between different mobsims, but that seems to be gone.  kai,
	 * feb'25</li>
	 * </ul></p>
	 */
	public double getRelativePositionOfEntryExitOnLink() {
		return 1.;
	}
	// ---
	private long randomSeed = 4711L;
	private static final String RANDOM_SEED = "randomSeed";
	@StringGetter( RANDOM_SEED )
	public long getRandomSeed() {
		return this.randomSeed;
	}
	@StringSetter( RANDOM_SEED )
	public void setRandomSeed(final long randomSeed) {
		this.randomSeed = randomSeed;
	}
	// ---
	@PositiveOrZero
	private int numberOfThreads = 2;
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String NUMBER_OF_THREADS_CMT = "\"global\" number of threads.  "
			+ "This number is used, e.g., for replanning, but NOT in QSim.  "
			+ "This can typically be set to as many cores as you have available, or possibly even slightly more.";

    private static final String DEFAULT_DELIMITER = "defaultDelimiter";
    private static final String DEFAULT_DELIMITER_CMT = "Default Delimiter for CSV files. May not be recognized by all writers.";
    @NotBlank
    private String defaultDelimiter = ";";

	/**
	 * @return {@link #NUMBER_OF_THREADS_CMT}
	 */
	@StringGetter( NUMBER_OF_THREADS )
	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}
	/**
	 * @param numberOfThreads -- {@link #NUMBER_OF_THREADS_CMT}
	 */
	@StringSetter( NUMBER_OF_THREADS )
	public void setNumberOfThreads(final int numberOfThreads) {
		log.info("setting number of threads to: " + numberOfThreads ) ; // might not be so bad to do this everywhere?  benjamin/kai, oct'10
		this.numberOfThreads = numberOfThreads;
	}
	// ---
	private String coordinateSystem = "Atlantis" ;
	// see https://matsim.atlassian.net/browse/MATSIM-898
	private static final String COORDINATE_SYSTEM = "coordinateSystem";
	@StringGetter( COORDINATE_SYSTEM )
	public String getCoordinateSystem() {
		return this.coordinateSystem;
	}
	@StringSetter( COORDINATE_SYSTEM )
	public void setCoordinateSystem(final String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}
	// ---
	private boolean insistingOnDeprecatedConfigVersion = true ;
	// yyyy this should be set to false eventually.  kai, aug'18
	private static final String INSITING_ON_DEPRECATED_CONFIG_VERSION = "insistingOnDeprecatedConfigVersion" ;
	@StringGetter( INSITING_ON_DEPRECATED_CONFIG_VERSION )
	public final boolean isInsistingOnDeprecatedConfigVersion() { return this.insistingOnDeprecatedConfigVersion ; }
	@StringSetter( INSITING_ON_DEPRECATED_CONFIG_VERSION )
	public final void setInsistingOnDeprecatedConfigVersion( boolean val ) {
		this.insistingOnDeprecatedConfigVersion = val ;
	}

    @StringGetter(DEFAULT_DELIMITER)
    public String getDefaultDelimiter() {
        return defaultDelimiter;
    }

    @StringSetter(DEFAULT_DELIMITER)
    public void setDefaultDelimiter(String defaultDelimiter) {
        this.defaultDelimiter = defaultDelimiter;
    }
}
