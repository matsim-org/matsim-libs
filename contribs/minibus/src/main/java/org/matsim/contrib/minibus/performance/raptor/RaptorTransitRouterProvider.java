/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.performance.raptor;

import javax.inject.Inject;
import javax.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Created by amit on 04.01.18.
 */

@Singleton
public class RaptorTransitRouterProvider implements Provider<TransitRouter> {

    private final RaptorDisutility raptorDisutility;
    private final TransitRouterConfig transitRouterConfig;
    private final TransitSchedule schedule;

    @Inject
    RaptorTransitRouterProvider(final TransitSchedule schedule, final Config config) {
        //TODO : These numbers are set in PConfigGroup. Need to provide a way to set these numbers if someone does not want to use PConfigGroup?
        double costPerMeterTraveled = 0.;
        double costPerBoarding = 0.;

        this.transitRouterConfig = new TransitRouterConfig(
                config.planCalcScore(),
                config.plansCalcRoute(),
                config.transitRouter(),
                config.vspExperimental());
        this.raptorDisutility = new RaptorDisutility(this.transitRouterConfig,
                costPerBoarding, costPerMeterTraveled);
        this.schedule = schedule;
    }

    @Override
    public TransitRouter get() {
        return new Raptor(transitRouterConfig, this.schedule, this.raptorDisutility );
    }
}
