/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.fiss;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;

import java.util.Collections;
import java.util.Set;

/**
 * @author nkuehnel / MOIA, hrewald
 */
public class FISSConfigGroup extends ReflectiveConfigGroup {
	private static final Logger LOG = LogManager.getLogger( FISSConfigGroup.class );

	public static final String GROUP_NAME = "fiss";

	@Parameter
	@Comment("Defines the share of agents that should be explicitly assigned in the QSim. " +
				 "Values between (0,1]")
	@Positive
	public double sampleFactor = 1.; // TODO: sample factors by mode?

	@Parameter
	@Comment("Defines the mods that will be considered for the FISS. Defaults to {car}")
	@NotNull
	public Set<String> sampledModes = Collections.singleton(TransportMode.car);

	@Parameter
	@Comment("Disable FISS in the last iteration to get events of all agents. May be required for post-processing")
	public boolean switchOffFISSLastIteration = true;

	public FISSConfigGroup() {
		super(GROUP_NAME);
	}

	@Override protected void checkConsistency( Config config ){
		super.checkConsistency( config );

		switch( config.qsim().getVehicleBehavior() ){
			case teleport -> {
			}
			default -> {
				throw new RuntimeException( "FISS only works together with vehicle behavior=teleport.  See code for more info." );
				// This was previously implemented such that it also ran through with other settings.  However, it would teleport the
				// vehicle immediately to its destination, thus leading to a faulty physical modelling of "wait" or "exception".   I
				// can't say if a possibly waiting agent would wait for the driver of the vehicle, or for the vehicle itself; this
				// would need to be checked.  kai, feb'25
			}
		}

		if( !config.qsim().getVehiclesSource().equals( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData ) ){
			throw new IllegalArgumentException( "For the time being, FISS only works with mode vehicle types from vehicles data, please check config!" );
			// reason is that FISS changes the PCE in the mode vehicles.
		}

		for( String sampledMode : sampledModes ){
			if( !config.qsim().getMainModes().contains( sampledMode ) ){
				final String message = sampledMode + " is not a qsim mode, it cannot apply FISS, please remove that mode from the list of qsim modes";
				LOG.fatal( message );
				throw new RuntimeException( message );
			}
		}


	}
}
