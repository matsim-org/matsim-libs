/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultMobsimModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.mobsim;

import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.MobsimScopeEventHandlingModule;
import org.matsim.core.mobsim.hermes.HermesProvider;
import org.matsim.core.mobsim.qsim.QSimModule;

public class DefaultMobsimModule extends AbstractModule {
    @Override
    public void install() {
        if (getConfig().controller().getMobsim().equals(ControllerConfigGroup.MobsimType.qsim.toString())) {
            install(new QSimModule());
//            bind(  RelativePositionOfEntryExitOnLink.class ).toInstance( () -> 1. );
        } else if (getConfig().controller().getMobsim().equals("JDEQSim")) {
						throw new IllegalArgumentException("JDEQSim is no longer supported as a mobsim. / March 2025");
        } else if (getConfig().controller().getMobsim().equals(ControllerConfigGroup.MobsimType.hermes.toString())) {
            bindMobsim().toProvider(HermesProvider.class);
        }

        install(new MobsimScopeEventHandlingModule());
    }
//    public interface RelativePositionOfEntryExitOnLink{
//        double get() ;
//    }
}
