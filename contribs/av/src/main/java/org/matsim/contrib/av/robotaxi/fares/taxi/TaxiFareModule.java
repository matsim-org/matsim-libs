/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.av.robotaxi.fares.taxi;/*
 * created by jbischoff, 11.12.2018
 */

import org.matsim.core.controler.AbstractModule;

public class TaxiFareModule extends AbstractModule {
    @Override
    public void install() {
        if ((getConfig().getModules().containsKey(TaxiFareConfigGroup.GROUP_NAME)) && (getConfig().getModules().containsKey(TaxiFaresConfigGroup.GROUP_NAME))) {
            throw new RuntimeException("Both taxifare and taxifares - config groups are specified. Please use only one of them in your config file and restart");
        }
        if (getConfig().getModules().containsKey(TaxiFareConfigGroup.GROUP_NAME)) {
            addEventHandlerBinding().toInstance(new TaxiFareHandler(TaxiFareConfigGroup.get(getConfig())));
        } else {
            TaxiFaresConfigGroup taxiFaresConfigGroup = TaxiFaresConfigGroup.get(getConfig());
            taxiFaresConfigGroup.getTaxiFareConfigGroups().forEach(taxiFareConfigGroup -> addEventHandlerBinding().toInstance(new TaxiFareHandler(taxiFareConfigGroup)));
        }
    }
}
