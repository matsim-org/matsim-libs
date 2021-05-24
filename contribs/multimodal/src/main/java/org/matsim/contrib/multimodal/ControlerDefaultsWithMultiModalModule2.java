/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MultiModalModule.java
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

package org.matsim.contrib.multimodal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Map;

public class ControlerDefaultsWithMultiModalModule2 extends AbstractModule {

    private MultiModalModule delegate = new MultiModalModule();

    @Override
    public void install() {
        install(AbstractModule.override(Arrays.<AbstractModule>asList(new ControlerDefaultsModule()), delegate));
    }

    public void setLinkSlopes(Map<Id<Link>, Double> linkSlopes) {
        this.delegate.setLinkSlopes(linkSlopes);
    }

    public void addAdditionalTravelTimeFactory(String mode, Provider<TravelTime> travelTimeFactory) {
        this.delegate.addAdditionalTravelTimeFactory(mode, travelTimeFactory);
    }


}
