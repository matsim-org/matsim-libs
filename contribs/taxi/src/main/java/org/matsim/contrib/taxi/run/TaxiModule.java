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

import org.matsim.contrib.dvrp.router.DynRoutingModule;
import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.optimizer.*;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vehicles.*;

import com.google.inject.name.Names;


public class TaxiModule
    extends AbstractModule
{
    public static final String TAXI_MODE = "taxi";

    private final TaxiData taxiData;
    private final VehicleType vehicleType;


    public TaxiModule(TaxiData taxiData, TaxiConfigGroup taxiCfg)
    {
        this(taxiData, VehicleUtils.getDefaultVehicleType());
    }


    public TaxiModule(TaxiData taxiData, VehicleType vehicleType)
    {
        this.taxiData = taxiData;
        this.vehicleType = vehicleType;
    }


    @Override
    public void install()
    {
        addRoutingModuleBinding(TAXI_MODE).toInstance(new DynRoutingModule(TAXI_MODE));
        bind(TaxiData.class).toInstance(taxiData);
        bind(VehicleType.class).annotatedWith(Names.named(TAXI_MODE)).toInstance(vehicleType);
        bind(TaxiOptimizerFactory.class).to(DefaultTaxiOptimizerFactory.class);

        addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
        addControlerListenerBinding().to(TaxiStatsDumper.class);
    }
}
