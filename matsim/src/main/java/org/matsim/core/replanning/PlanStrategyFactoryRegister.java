/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.replanning;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;

/**
 * Just here as a service for playground code.
 * Presents the DefaultPlanStrategiesModule so you can get a PlanStrategyFactory by name.
 */
public class PlanStrategyFactoryRegister {

	public static PlanStrategyFactory getInstance(final String strategyType) {
	    return new PlanStrategyFactory() {
            @Override
            public PlanStrategy get() {
                Injector injector = Injector.createInjector(ConfigUtils.createConfig(), new AbstractModule() {
                    @Override
                    public void install() {
                        include(new DefaultPlanStrategiesModule());
                    }
                });
                return injector.getPlanStrategiesDeclaredByModules().get(strategyType);
            }
        };
	}

}
