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

package org.mjanowski.master;

import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ExternalMobimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.qsim.QSimModule;

public class MasterMobsimModule extends AbstractModule {
    @Override
    public void install() {
            install(new MasterMobsimModule());
    }

}
