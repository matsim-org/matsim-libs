/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SignalsModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package org.matsim.contrib.signals.controler;

import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

public class SignalsModule extends AbstractModule {
    @Override
    public void install() {
        if ((boolean) ConfigUtils.addOrGetModule(getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).isUseSignalSystems()) {
            bind(SignalsControllerListener.class).to(DefaultSignalsControllerListener.class);
            addControlerListenerBinding().to(SignalsControllerListener.class);
        }
    }
}
