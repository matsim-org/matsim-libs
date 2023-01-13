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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Collections;
import java.util.Set;

/**
 * @author nkuehnel / MOIA, hrewald
 */
public class FISSConfigGroup extends ReflectiveConfigGroup {

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

}
