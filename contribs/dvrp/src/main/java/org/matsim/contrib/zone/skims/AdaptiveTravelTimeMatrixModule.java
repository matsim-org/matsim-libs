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

package org.matsim.contrib.zone.skims;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;

/**
 * @author steffenaxer
 */
public class AdaptiveTravelTimeMatrixModule extends AbstractDvrpModeModule {
    private final double ALTERNATIVE_ENDTIME = 30*3600;
    private final static double SMOOTHING_ALPHA = 0.75; // High weight to new values

    @Inject
    private QSimConfigGroup qsimConfig;

    @Inject
    private DvrpConfigGroup dvrpConfigGroup;

    public AdaptiveTravelTimeMatrixModule(String mode) {
        super(mode);
    }

    @Override
    public void install() {
        bindModal(AdaptiveTravelTimeMatrix.class).toProvider(modalProvider(
                getter -> {
					Network network = getter.getModal(Network.class);
					DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
					ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network,
						matrixParams.getZoneSystemParams(), getConfig().global().getCoordinateSystem(), zone -> true);
                    return new AdaptiveTravelTimeMatrixImpl(qsimConfig.getEndTime().orElse(ALTERNATIVE_ENDTIME),
                            network,
							zoneSystem,
                            matrixParams,
                            getter.getModal(TravelTimeMatrix.class), SMOOTHING_ALPHA);
                }))
                .in(Singleton.class);
    }
}
