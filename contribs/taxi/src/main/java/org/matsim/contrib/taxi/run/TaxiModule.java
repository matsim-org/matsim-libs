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

package org.matsim.contrib.taxi.run;

import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.router.TaxiRoutingModule;
import org.matsim.contrib.taxi.scheduler.*;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Singleton;


public class TaxiModule
    extends AbstractModule
{
    public static final String TAXI_MODE = "taxi";


    @Override
    public void install()
    {
        bindMobsim().toProvider(TaxiQSimProvider.class);//TODO ??????
        addRoutingModuleBinding(TaxiModule.TAXI_MODE).to(TaxiRoutingModule.class);

        bind(VrpData.class).to(TaxiData.class).in(Singleton.class);

        //??????????????TODO
        //bind(TravelTime)
        //bind(TaxiSchedulerParams.class)
        //bind(AbstractTaxiOptimizerParams)
    }
}
