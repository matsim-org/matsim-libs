/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.config;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Set;

/**
 * Config of the Railsim contrib.
 */
public final class RailsimConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "railsim";

	@Parameter
	@Comment("Comma separated set of modes that are handled by the railsim qsim engine in the simulation. Defaults to 'rail'.")
	public String networkModes = "rail";

	@Parameter
	@Comment("Global acceleration in meters per second^2 which is used if there is no value provided in the vehicle attributes (" + RailsimUtils.VEHICLE_ATTRIBUTE_ACCELERATION + ");" + " used to compute the train velocity per link.")
	public double accelerationDefault = 0.5;

	@Parameter
	@Comment("Global deceleration in meters per second^2 which is used if there is no value provided in the vehicle attributes (" + RailsimUtils.VEHICLE_ATTRIBUTE_DECELERATION + ");" + " used to compute the reserved train path and the train velocity per link.")
	public double decelerationDefault = 0.5;

	@Parameter
	@Comment("Time interval in seconds a train has to wait until trying again to request a track reservation if the track was blocked by another train.")
	public double pollInterval = 10;

	@Parameter
	@Comment("Maximum time interval in seconds which is used to update the train position update events.")
	public double updateInterval = 10.;

	public RailsimConfigGroup() {
		super(GROUP_NAME);
	}

	public Set<String> getNetworkModes() {
		return Set.of(networkModes.split(","));
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		for (String mode : getNetworkModes()) {
			if (config.qsim().getMainModes().contains(mode)) {
				throw new IllegalArgumentException(String.format("Railsim mode '%s' must not be a network mode in qsim.", mode));
			}
		}
	}

}
