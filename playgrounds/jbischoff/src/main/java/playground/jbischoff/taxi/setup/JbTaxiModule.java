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

package playground.jbischoff.taxi.setup;

import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vehicles.*;

import com.google.inject.name.Names;


public class JbTaxiModule
    extends AbstractModule
{
    public static final String TAXI_MODE = "taxi";

    private final Fleet taxiData;
    private final VehicleType vehicleType;


    public JbTaxiModule(Fleet taxiData)
    {
        this(taxiData, VehicleUtils.getDefaultVehicleType());
    }


    public JbTaxiModule(Fleet taxiData, VehicleType vehicleType)
    {
        this.taxiData = taxiData;
        this.vehicleType = vehicleType;
    }


    @Override
    public void install()
    {
        bind(Fleet.class).toInstance(taxiData);
        bind(VehicleType.class).annotatedWith(Names.named(TAXI_MODE)).toInstance(vehicleType);
		bind(SubmittedTaxiRequestsCollector.class).toInstance(new SubmittedTaxiRequestsCollector());

        //addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
//        addControlerListenerBinding().to(TaxiStatsDumper.class);

        if (TaxiConfigGroup.get(getConfig()).getTimeProfiles()) {
            addMobsimListenerBinding().toProvider(TaxiStatusTimeProfileCollectorProvider.class);
            //add more time profiles if necessary
        }
    }
}
