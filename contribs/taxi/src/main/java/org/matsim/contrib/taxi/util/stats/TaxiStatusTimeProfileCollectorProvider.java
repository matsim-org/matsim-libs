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

package org.matsim.contrib.taxi.util.stats;

import org.matsim.contrib.taxi.data.TaxiData;
import org.matsim.contrib.taxi.data.TaxiRequest.TaxiRequestStatus;
import org.matsim.contrib.taxi.util.stats.TimeProfileCollector.ProfileCalculator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import com.google.inject.*;


public class TaxiStatusTimeProfileCollectorProvider
    implements Provider<MobsimListener>
{
    private final TaxiData taxiData;
    private final MatsimServices matsimServices;


    @Inject
    public TaxiStatusTimeProfileCollectorProvider(TaxiData taxiData, MatsimServices matsimServices)
    {
        this.taxiData = taxiData;
        this.matsimServices = matsimServices;
    }


    @Override
    public MobsimListener get()
    {
        ProfileCalculator calc = TimeProfiles.combineProfileCalculators(
                TaxiTimeProfiles.createCurrentTaxiTaskOfTypeCounter(taxiData), //
                TaxiTimeProfiles.createRequestsWithStatusCounter(taxiData,
                        TaxiRequestStatus.UNPLANNED));
        return new TimeProfileCollector(calc, 300, "taxi_status_time_profiles", matsimServices);
    }
}
