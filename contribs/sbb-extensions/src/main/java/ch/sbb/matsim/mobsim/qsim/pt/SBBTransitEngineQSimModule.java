/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
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
package ch.sbb.matsim.mobsim.qsim.pt;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;

/**
 * @author Sebastian Hörl / ETHZ
 */
public class SBBTransitEngineQSimModule extends AbstractQSimModule implements QSimComponentsConfigurator {

    public static final String COMPONENT_NAME = "SBBTransit";

    @Override
    public void configure(QSimComponentsConfig components) {
        if (components.hasNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME)) {
            components.removeNamedComponent(TransitEngineModule.TRANSIT_ENGINE_NAME);
        }

        components.addNamedComponent(COMPONENT_NAME);
    }

    @Override
    protected void configureQSim() {
        bind(SBBTransitQSimEngine.class).asEagerSingleton();
        addQSimComponentBinding(COMPONENT_NAME).to(SBBTransitQSimEngine.class);
    }
}
