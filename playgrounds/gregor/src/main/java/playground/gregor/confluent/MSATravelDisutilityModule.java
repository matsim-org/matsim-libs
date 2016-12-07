package playground.gregor.confluent;/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import com.google.inject.Injector;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;

import javax.inject.Inject;

public class MSATravelDisutilityModule extends TravelDisutilityModule {
    @Inject
    Injector injector;

    @Override
    public void install() {
        addEventHandlerBinding().to(MSATravelDisutility.class);
        PlansCalcRouteConfigGroup routeConfigGroup = getConfig().plansCalcRoute();
        MSATravelDisutilityFactory fac = injector.getInstance(MSATravelDisutilityFactory.class);
        for (String mode : routeConfigGroup.getNetworkModes()) {
            addTravelDisutilityFactoryBinding(mode).toInstance(fac);
        }
    }
}
